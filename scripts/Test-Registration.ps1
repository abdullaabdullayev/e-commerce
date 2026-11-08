param(
    [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $JavaHome) { throw 'Set JAVA_HOME to JDK 21 or pass -JavaHome.' }
$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw 'Java executable was not found.' }

$authJar = Get-ChildItem (Join-Path $projectRoot 'auth-service/build/libs') -Filter '*.jar' |
    Where-Object Name -NotLike '*-plain.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$userJar = Get-ChildItem (Join-Path $projectRoot 'user-service/build/libs') -Filter '*.jar' |
    Where-Object Name -NotLike '*-plain.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $authJar -or -not $userJar) { throw 'Build :auth-service:bootJar and :user-service:bootJar first.' }

$runId = [guid]::NewGuid().ToString('N').Substring(0, 12)
$testPassword = [guid]::NewGuid().ToString('N')
$logDirectory = Join-Path $projectRoot "build/registration-check/$runId"
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$containers = [System.Collections.Generic.List[string]]::new()
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()

function Invoke-Docker {
    $output = & docker @args
    if ($LASTEXITCODE -ne 0) { throw "Docker operation '$($args[0])' failed." }
    return $output
}

function New-TestContainer {
    param([string]$Suffix, [string[]]$Options, [string]$Image)
    $containerId = Invoke-Docker run --detach --name "registration-$runId-$Suffix" @Options $Image
    $containerId = ($containerId | Select-Object -Last 1).Trim()
    if ($containerId -notmatch '^[a-f0-9]{64}$') { throw 'Unexpected container identifier.' }
    $containers.Add($containerId)
    return $containerId
}

function Wait-Until {
    param([string]$Description, [scriptblock]$Condition, [int]$TimeoutSeconds = 90)
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastFailure = ''
    do {
        try { if (& $Condition) { return } }
        catch { $lastFailure = $_.Exception.Message }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Timed out: $Description. $lastFailure"
}

function Get-MappedPort {
    param([string]$Container, [int]$Port)
    $mapping = Invoke-Docker port $Container "$Port/tcp"
    return [int](($mapping | Select-Object -First 1) -split ':')[-1]
}

function Get-FreePort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return $listener.LocalEndpoint.Port } finally { $listener.Stop() }
}

function Get-Count {
    param([string]$Container, [string]$Database, [string]$Sql)
    return [int]((Invoke-Docker exec $Container psql -U registration_test -d $Database -tAc $Sql) -join '').Trim()
}

function Start-TestService {
    param([string]$Name, [string]$Jar, [int]$Port, [int]$DatabasePort, [string]$Database)
    $arguments = @(
        '-Xmx256m', '-jar', "`"$Jar`"", "--server.port=$Port", '--server.address=127.0.0.1',
        "--spring.datasource.url=jdbc:postgresql://127.0.0.1:$DatabasePort/$Database",
        '--spring.datasource.username=registration_test', "--spring.datasource.password=$testPassword",
        '--spring.jpa.hibernate.ddl-auto=create-drop', '--spring.jpa.show-sql=false',
        '--spring.rabbitmq.host=127.0.0.1', "--spring.rabbitmq.port=$rabbitPort",
        '--spring.rabbitmq.username=registration_test', "--spring.rabbitmq.password=$testPassword",
        '--spring.rabbitmq.connection-timeout=2s', '--spring.rabbitmq.requested-heartbeat=2s',
        '--spring.rabbitmq.listener.simple.concurrency=1',
        '--eureka.client.enabled=false', '--spring.cloud.discovery.enabled=false',
        '--app.outbox.poll-delay-ms=200', '--app.outbox.confirm-timeout-ms=2000',
        "--spring.jwt.secret=$testPassword", '--spring.jwt.access-expiration=60000',
        '--spring.jwt.refresh-expiration=120000'
    )
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $projectRoot `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logDirectory "$Name.log") `
        -RedirectStandardError (Join-Path $logDirectory "$Name.err.log")
    $processes.Add($process)
    Wait-Until "$Name HTTP listener" {
        if ($process.HasExited) { throw "$Name exited; see $logDirectory" }
        $client = [System.Net.Sockets.TcpClient]::new()
        try { $client.Connect('127.0.0.1', $Port); return $client.Connected }
        finally { $client.Dispose() }
    }
}

function Register-Account {
    param([string]$Email)
    $body = @{ email = $Email; password = 'Registration-test-123!'; firstName = 'Test'; lastName = 'Parent' } |
        ConvertTo-Json
    $response = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "http://127.0.0.1:$authPort/api/v1/auth/register" `
        -ContentType 'application/json' -Body $body -TimeoutSec 20
    if ($response.StatusCode -ne 201) { throw 'Registration did not return HTTP 201.' }
    return $response.Content | ConvertFrom-Json
}

try {
    Write-Output 'Starting isolated PostgreSQL and RabbitMQ containers...'
    $authDb = New-TestContainer 'auth-db' @('-p', '127.0.0.1::5432', '-e', 'POSTGRES_DB=auth_db',
        '-e', 'POSTGRES_USER=registration_test', '-e', "POSTGRES_PASSWORD=$testPassword") 'postgres:16-alpine'
    $userDb = New-TestContainer 'user-db' @('-p', '127.0.0.1::5432', '-e', 'POSTGRES_DB=user_db',
        '-e', 'POSTGRES_USER=registration_test', '-e', "POSTGRES_PASSWORD=$testPassword") 'postgres:16-alpine'
    $rabbit = New-TestContainer 'rabbit' @('-p', '127.0.0.1::5672', '-p', '127.0.0.1::15672',
        '-e', 'RABBITMQ_DEFAULT_USER=registration_test', '-e', "RABBITMQ_DEFAULT_PASS=$testPassword") 'rabbitmq:3-management-alpine'
    $rabbitPort = Get-MappedPort $rabbit 5672
    $managementPort = Get-MappedPort $rabbit 15672
    $authDbPort = Get-MappedPort $authDb 5432
    $userDbPort = Get-MappedPort $userDb 5432
    $authPort = Get-FreePort
    $userPort = Get-FreePort
    while ($userPort -eq $authPort) { $userPort = Get-FreePort }
    $basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("registration_test:$testPassword"))
    $managementHeaders = @{ Authorization = "Basic $basic" }
    $managementUrl = "http://127.0.0.1:$managementPort/api"

    # TCP readiness excludes the temporary Unix-socket-only server used by initdb.
    Wait-Until 'auth PostgreSQL' {
        & docker exec $authDb pg_isready -h 127.0.0.1 -q -U registration_test -d auth_db 2>$null
        return $LASTEXITCODE -eq 0
    }
    Wait-Until 'user PostgreSQL' {
        & docker exec $userDb pg_isready -h 127.0.0.1 -q -U registration_test -d user_db 2>$null
        return $LASTEXITCODE -eq 0
    }
    Wait-Until 'RabbitMQ management API' {
        $null = Invoke-RestMethod "$managementUrl/overview" -Headers $managementHeaders -TimeoutSec 3
        return $true
    }
    Write-Output 'Starting User and Auth services on temporary ports...'
    Start-TestService 'user' $userJar.FullName $userPort $userDbPort 'user_db'
    Start-TestService 'auth' $authJar.FullName $authPort $authDbPort 'auth_db'
    $queueUrl = "$managementUrl/queues/%2F/user-service.user-registered"
    Wait-Until 'User consumer subscription' {
        (Invoke-RestMethod $queueUrl -Headers $managementHeaders -TimeoutSec 3).consumers -ge 1
    }

    $first = Register-Account 'normal@example.test'
    $firstId = [long]$first.accountId
    Wait-Until 'normal registration profile' {
        (Get-Count $userDb 'user_db' "SELECT COUNT(*) FROM users WHERE account_id=$firstId") -eq 1
    }
    Write-Output 'PASS: normal registration creates an account and a profile.'

    Invoke-Docker exec $rabbit rabbitmqctl stop_app | Out-Null
    $offline = Register-Account 'broker-offline@example.test'
    $offlineId = [long]$offline.accountId
    Wait-Until 'failed delivery persisted in outbox' {
        (Get-Count $authDb 'auth_db' "SELECT COUNT(*) FROM auth_outbox WHERE payload::jsonb->>'accountId'='$offlineId' AND published_at IS NULL AND attempts>0") -eq 1
    }
    if ((Get-Count $authDb 'auth_db' "SELECT COUNT(*) FROM accounts WHERE id=$offlineId") -ne 1) {
        throw 'Account was not committed during broker outage.'
    }
    if ((Get-Count $userDb 'user_db' "SELECT COUNT(*) FROM users WHERE account_id=$offlineId") -ne 0) {
        throw 'Profile unexpectedly exists while broker is stopped.'
    }
    Write-Output 'PASS: registration succeeds during broker outage; the event stays pending.'

    Invoke-Docker exec $rabbit rabbitmqctl start_app | Out-Null
    Wait-Until 'recovered broker delivery' {
        (Get-Count $userDb 'user_db' "SELECT COUNT(*) FROM users WHERE account_id=$offlineId") -eq 1
    }
    Wait-Until 'published outbox status' {
        (Get-Count $authDb 'auth_db' 'SELECT COUNT(*) FROM auth_outbox WHERE published_at IS NULL') -eq 0
    }
    Write-Output 'PASS: broker recovery delivers the pending event and creates the profile.'

    $payload = @{ accountId = $firstId; email = 'normal@example.test'; firstName = 'Test'; lastName = 'Parent' } |
        ConvertTo-Json -Compress
    $message = @{
        properties = @{ content_type = 'application/json'; delivery_mode = 2; message_id = "duplicate-$runId" }
        routing_key = 'user.registered'; payload = $payload; payload_encoding = 'string'
    } | ConvertTo-Json -Depth 5 -Compress
    1..2 | ForEach-Object {
        $result = Invoke-RestMethod "$managementUrl/exchanges/%2F/auth.events/publish" -Method Post `
            -Headers $managementHeaders -ContentType 'application/json' -Body $message -TimeoutSec 5
        if (-not $result.routed) { throw 'Duplicate test message was not routed.' }
    }
    # A marker follows the duplicates on the same queue with a single consumer.
    # Its profile proves the preceding messages have been handled, without relying
    # on management statistics that are sampled and can reset on broker restart.
    $marker = Register-Account 'after-duplicates@example.test'
    $markerId = [long]$marker.accountId
    Wait-Until 'consumer passed the duplicate messages' {
        (Get-Count $userDb 'user_db' "SELECT COUNT(*) FROM users WHERE account_id=$markerId") -eq 1
    }
    if ((Get-Count $userDb 'user_db' "SELECT COUNT(*) FROM users WHERE account_id=$firstId") -ne 1) {
        throw 'Duplicate delivery created additional profiles.'
    }
    # Invoke-RestMethod returns the JSON array as one pipeline object. Wrapping the
    # command in @() would make an empty JSON array look like one dead letter.
    $deadLetters = Invoke-RestMethod "$managementUrl/queues/%2F/user-service.user-registered.dlq/get" `
        -Method Post -Headers $managementHeaders -ContentType 'application/json' `
        -Body '{"count":10,"ackmode":"ack_requeue_true","encoding":"auto"}' -TimeoutSec 5
    if ($deadLetters.Count -ne 0) { throw 'Messages were rejected into the dead-letter queue.' }
    Write-Output 'PASS: two duplicate messages are acknowledged without creating another profile.'
    Write-Output "All four registration scenarios passed. Logs: $logDirectory"
}
finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
        $process.Dispose()
    }
    # IDs come only from containers created above; no project volumes are mounted or removed.
    foreach ($containerId in $containers) {
        & docker rm --force --volumes $containerId | Out-Null
    }
}
