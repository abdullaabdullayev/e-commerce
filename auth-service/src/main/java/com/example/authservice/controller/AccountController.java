package com.example.authservice.controller;

import com.example.authservice.model.*;
import com.example.authservice.repository.AccountRepository;
import com.example.authservice.messaging.UserEventPublisher;
import com.example.user.api.event.UserRegisteredEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@Validated
public class AccountController {
    private final AccountRepository accounts;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final UserEventPublisher events;
    private final String bootstrapKey;
    public AccountController(AccountRepository accounts,JdbcTemplate jdbc,PasswordEncoder passwords,UserEventPublisher events,@Value("${auth.bootstrap-key}") String bootstrapKey) {
        this.accounts=accounts; this.jdbc=jdbc; this.passwords=passwords; this.events=events; this.bootstrapKey=bootstrapKey;
    }
    public record AccountView(long accountId,String email,String username,boolean active,Set<Role> roles) {}
    public record RolesInput(@NotEmpty Set<@NotNull Role> roles) {}
    public record BootstrapInput(@Positive long accountId) {}
    public record CreateAccount(@Email @Size(max=254) String email,@Pattern(regexp="[a-z][a-z0-9._-]{2,39}") String username,
                                @NotBlank @Size(min=8,max=72) String password,@NotBlank @Size(max=80) String firstName,
                                @NotBlank @Size(max=80) String lastName,@NotEmpty Set<@NotNull Role> roles) {
        @AssertTrue public boolean isIdentityValid() { return (email!=null&&!email.isBlank()) || username!=null; }
        @AssertTrue public boolean isPasswordWithinByteLimit() { return password==null || password.getBytes(StandardCharsets.UTF_8).length<=72; }
    }
    private Account require(long id) { return accounts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private AccountView view(Account a) { return new AccountView(a.getId(),a.getEmail(),a.getUsername(),a.isActive(),Set.copyOf(a.getRoles())); }
    @GetMapping("/api/v1/auth/me") public AccountView me(Authentication auth) { return view(accounts.findByIdentifier(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED))); }
    @GetMapping({"/api/v1/auth/accounts/{id}", "/api/v1/admin/accounts/{id}"}) public AccountView get(@PathVariable @Positive long id) { return view(require(id)); }
    @GetMapping({"/api/v1/auth/accounts", "/api/v1/admin/accounts"}) public List<AccountView> list(@RequestParam(required=false) Role role,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page) {
        List<Long> ids=role==null ? jdbc.queryForList("SELECT id FROM accounts ORDER BY id LIMIT 50 OFFSET ?",Long.class,page*50)
                : jdbc.queryForList("SELECT account_id FROM account_roles WHERE role=? ORDER BY account_id LIMIT 50 OFFSET ?",Long.class,role.name(),page*50);
        return accounts.findAllById(ids).stream().sorted(Comparator.comparing(Account::getId)).map(this::view).toList();
    }
    @PostMapping({"/api/v1/auth/accounts", "/api/v1/admin/accounts"}) @ResponseStatus(HttpStatus.CREATED) @Transactional
    public AccountView create(@Valid @RequestBody CreateAccount r) {
        String email=r.email()==null || r.email().isBlank() ? null : r.email().trim();
        if(email!=null && accounts.existsByEmail(email) || r.username()!=null && accounts.existsByUsername(r.username())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email or username already exists");
        Account account=new Account(); account.setEmail(email); account.setUsername(r.username()); account.setRoles(new HashSet<>(r.roles())); account.setPasswordHash(passwords.encode(r.password()));
        account=accounts.saveAndFlush(account);
        events.publishUserRegistered(new UserRegisteredEvent(account.getId(),account.getEmail(),r.firstName(),r.lastName()));
        return view(account);
    }
    private void roleLock() { jdbc.execute("SELECT pg_advisory_xact_lock(82350001)"); }
    @PutMapping({"/api/v1/auth/accounts/{id}/roles", "/api/v1/admin/accounts/{id}/roles"}) @Transactional
    public AccountView roles(@PathVariable @Positive long id,@Valid @RequestBody RolesInput r) {
        roleLock(); Account account=require(id);
        if(account.getRoles().contains(Role.ADMIN) && !r.roles().contains(Role.ADMIN)
                && jdbc.queryForObject("SELECT count(*) FROM account_roles WHERE role='ADMIN'",Integer.class)<=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"The last administrator cannot be removed");
        account.setRoles(new HashSet<>(r.roles())); return view(accounts.save(account));
    }
    @PostMapping("/api/v1/auth/bootstrap-admin") @Transactional
    public AccountView bootstrap(@RequestHeader(value="X-Admin-Bootstrap-Key",required=false) String supplied,@Valid @RequestBody BootstrapInput r) {
        if(bootstrapKey.length()<32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Administrator bootstrap is disabled");
        if(supplied==null || !MessageDigest.isEqual(bootstrapKey.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        roleLock(); if(jdbc.queryForObject("SELECT count(*) FROM account_roles WHERE role='ADMIN'",Integer.class)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"An administrator already exists");
        Account account=require(r.accountId()); if(!account.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Account is inactive");
        account.getRoles().add(Role.ADMIN); return view(accounts.save(account));
    }
}
