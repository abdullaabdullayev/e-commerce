package com.example.authservice.controller;

import com.example.authservice.dto.AccountDtos.*;
import com.example.authservice.model.Role;
import com.example.authservice.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accounts;

    @GetMapping("/api/v1/auth/me")
    public AccountView me(Authentication authentication) { return accounts.me(authentication.getName()); }

    @GetMapping({"/api/v1/auth/accounts/{id}", "/api/v1/admin/accounts/{id}"})
    public AccountView get(@PathVariable @Positive long id) { return accounts.get(id); }

    @GetMapping({"/api/v1/auth/accounts", "/api/v1/admin/accounts"})
    public List<AccountView> list(@RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "") @Size(max = 254) String q,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
        return accounts.list(role, q, page);
    }

    @PostMapping({"/api/v1/auth/accounts", "/api/v1/admin/accounts"})
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@Valid @RequestBody CreateAccount request) { return accounts.create(request); }

    @PutMapping({"/api/v1/auth/accounts/{id}/roles", "/api/v1/admin/accounts/{id}/roles"})
    public AccountView roles(@PathVariable @Positive long id, @Valid @RequestBody RolesInput request) {
        return accounts.replaceRoles(id, request.roles());
    }

    @PutMapping({"/api/v1/auth/accounts/{id}/active", "/api/v1/admin/accounts/{id}/active"})
    public AccountView active(@PathVariable @Positive long id, @Valid @RequestBody ActiveInput request) {
        return accounts.setActive(id, request.active());
    }

    @PostMapping("/api/v1/auth/bootstrap-admin")
    public AccountView bootstrap(@RequestHeader(value = "X-Admin-Bootstrap-Key", required = false) String supplied,
                                 @Valid @RequestBody BootstrapInput request) {
        return accounts.bootstrap(supplied, request.accountId());
    }
}
