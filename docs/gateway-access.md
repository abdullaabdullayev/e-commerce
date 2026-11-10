# Xarici giriş yalnız Gateway vasitəsilə

Əsas deployment `docker-compose.yaml` ilə idarə olunur. Gateway-dən başqa heç bir
konteynerin portu hosta publish edilmir. Bu məhdudiyyət Docker şəbəkəsində tətbiq olunur.
Header yoxlaması şəbəkə izolyasiyasını əvəz etmir.

```text
Brauzer / Postman → localhost:8080 → Gateway → daxili servislər
                                          → Auth, User, Product, Inventory, Order
```

## Şəbəkə

- `edge`: yalnız Gateway qoşulur; host portu standart olaraq `127.0.0.1:8080`-dir.
- `backend`: `internal: true`; bütün tətbiqlər, Eureka, bazalar və RabbitMQ buradadır.
- Gateway hər iki şəbəkəyə qoşulur. Başqa servislərdə `ports:` yoxdur.
- `8081`, `8083`, `8084`, `8085`, `8086`, `8761`, PostgreSQL, MongoDB və RabbitMQ
  portları Compose tərəfindən hosta açılmır.
- Servislər Docker DNS adları ilə əlaqə saxlayır: məsələn,
  `http://inventory-service:8084`, `http://discovery-service:8761/eureka/`.
- Backend servislərin internetə birbaşa çıxışı da bağlıdır. Gələcək ödəniş
  inteqrasiyası üçün yalnız ehtiyacı olan servisə nəzarətli çıxış əlavə edilməlidir.

`/internal/**` üçün Gateway marşrutu yoxdur. Inventory/Order daxili açarları və
kataloq admin açarı əlavə giriş nəzarəti kimi saxlanır. Admin əməliyyatları üçün
ictimai kataloq yolları Gateway üzərindən müvafiq admin açarı ilə çağırıla bilər.

## Başlatma

`.env` faylında `.env.example`-da göstərilən dəyişənləri konfiqurasiya et.
İlk dəfə faylı yaratmaq üçün mövcud `.env`-ni əvəz etmədən nümunəni kopyala və
boş secret sahələrini ayrıca təsadüfi dəyərlərlə doldur. SECRET_KEY ən azı 32 bayt olsun.
Əvvəlki PostgreSQL/RabbitMQ volume-ları varsa, giriş məlumatları onlara uyğun qalmalıdır;
environment dəyişmək mövcud bazanın parolunu dəyişmir.

```powershell
docker compose config --quiet
docker compose up -d --build
```

Dockerfile bütün tətbiqlərin bootJar fayllarını JDK 21 ilə hazırlayır; bu build
əmri test task-larını çağırmır. İlk build dependency və image endirmələri tələb edə bilər.
Servislər Eureka-da qeydiyyatdan keçənədək Gateway ilk sorğularda `503` qaytara bilər.

İstifadə edilən ünvanlar:

```text
POST http://localhost:8080/api/v1/auth/register
POST http://localhost:8080/api/v1/auth/login
GET  http://localhost:8080/api/v1/products
GET  http://localhost:8080/api/v1/categories
GET  http://localhost:8080/api/v1/stock/ARDUINO-KIT-01
GET  http://localhost:8080/api/v1/orders       (Bearer token tələb edir)
```

LAN/server girişini açmaq lazım olduqda `.env` daxilində yalnız
`GATEWAY_BIND_ADDRESS=0.0.0.0` dəyişdirilir. Digər servislərin portları açılmır.

## Tətbiq sərhədi

Bu qayda Compose ilə başlayan konteynerlər üçündür. Əvvəldən IDE/bootRun ilə
hostda işləyən tətbiqlər ayrıca proseslərdir; Docker konfiqurasiyası onların
listener-lərini bağlamır. Bu deployment-ə keçərkən həmin prosesləri dayandır.
Yeni konfiqurasiya mövcud konteynerlərə `docker compose up -d --build` ilə tətbiq olunur.
Docker administratoru və ya backend şəbəkəsinə qoşulma səlahiyyəti olan şəxs
daxili servislərə daxil ola bilər; bu, host administratorunu məhdudlaşdıran mexanizm deyil.

Volume-ların adları saxlanılıb. Keçid üçün `docker compose down -v` işlətmə;
bu, bazaların məlumatlarını silər. İnfrastrukturun idarə edilməsi üçün
`docker compose exec` istifadə edilə bilər; DB və idarəetmə portlarını publish etmək tələb olunmur.

Şəbəkə və publish qaydaları: [Docker Compose networking](https://docs.docker.com/compose/how-tos/networking/).

## IDE-dən başladılan Spring Boot üçün JDBC

Hostda işləyən tətbiq Docker DNS adlarını istifadə etmir. IDE üçün infrastrukturun
yalnız lokal portlarını açan ayrıca override mövcuddur:

```powershell
docker compose -f docker-compose.yaml -f compose.ide.yaml up -d postgres-auth postgres-user postgres-inventory postgres-order rabbitmq mongo
```

| Baza | IDE JDBC URL |
|---|---|
| Auth | `jdbc:postgresql://localhost:5432/auth_db` |
| User | `jdbc:postgresql://localhost:5433/user_db` |
| Inventory | `jdbc:postgresql://localhost:5434/inventory_db` |
| Order | `jdbc:postgresql://localhost:5435/order_db` |

IDE Run Configuration daxilində `.env`-dəki `DB_USERNAME` və `DB_PASSWORD` dəyərləri
ötürülməlidir. Docker servislərinə verilən `postgres-auth:5432` kimi JDBC URL-ləri
IDE Run Configuration-a köçürmə. Inventory/Order üçün Docker URL-ləri əvvəlcədən
IDE environment-də təyin edilibsə, onları yuxarıdakı localhost URL-ləri ilə əvəz et.

RabbitMQ `localhost:5672`, MongoDB `localhost:27017` vasitəsilə əlçatandır.
Eureka və tətbiq portları bu override-da hosta açılmır. Ayrı IDE instansiyasını
Docker registry-sinə qeydiyyatdan keçirmədən debug etmək üçün həmin Run
Configuration-a `EUREKA_CLIENT_ENABLED=false` əlavə et. Bu halda Gateway Docker-də
işləyən instansiyalara yönləndirməyə davam edir; IDE instansiyasına avtomatik keçmir.

Bu rejim yalnız localhost-a giriş verir; tətbiq servislərinin portları yenə bağlıdır.
İnfrastruktur əlavə `ide-access` bridge şəbəkəsinə qoşulur. Bu, yalnız `internal`
şəbəkəyə qoşulmuş konteynerlərdə Docker Desktop-un host portlarını aktivləşdirməməsi
halını həll edir. `backend` şəbəkəsi və tətbiq servislərinin izolyasiyası saxlanır.
Override-ı söndürmək üçün əsas faylla eyni infrastruktur servislərini yenidən yarat:

```powershell
docker compose -f docker-compose.yaml up -d postgres-auth postgres-user postgres-inventory postgres-order rabbitmq mongo
```

Hər iki keçid mövcud volume-ları saxlayır; konteynerlər yenidən yaradıldığı üçün
bağlantılarda qısa fasilə ola bilər.
