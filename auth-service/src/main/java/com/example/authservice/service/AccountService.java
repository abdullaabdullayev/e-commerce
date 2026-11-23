package com.example.authservice.service;

import com.example.authservice.dto.AccountDtos.*;
import com.example.authservice.messaging.UserEventPublisher;
import com.example.authservice.model.*;
import com.example.authservice.repository.AccountRepository;
import com.example.user.api.event.UserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final UserEventPublisher events;
    private final String bootstrapKey;

    public AccountService(AccountRepository accounts, PasswordEncoder passwords, UserEventPublisher events,
                          @Value("${auth.bootstrap-key}") String bootstrapKey) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.events = events;
        this.bootstrapKey = bootstrapKey;
    }

    private Account require(long id) {
        return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public AccountView me(String identifier) {
        return accounts.findByIdentifier(identifier).map(AccountView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public AccountView get(long id) { return AccountView.from(require(id)); }

    public List<AccountView> list(Role role, String query, int page) {
        var ids = accounts.searchIds(query.strip(), role == null, role == null ? Role.CUSTOMER : role, PageRequest.of(page, 50));
        return accounts.findAllById(ids).stream().sorted(Comparator.comparing(Account::getId)).map(AccountView::from).toList();
    }

    @Transactional
    public AccountView create(CreateAccount request) {
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim();
        if ((email != null && accounts.existsByEmail(email)) ||
                (request.username() != null && accounts.existsByUsername(request.username()))) {
            throw conflict("Email or username already exists");
        }
        if (request.roles().contains(Role.ADMIN)) accounts.lockRoleChanges();
        Account account = new Account();
        account.setEmail(email);
        account.setUsername(request.username());
        account.setRoles(new HashSet<>(request.roles()));
        account.setPasswordHash(passwords.encode(request.password()));
        account = accounts.saveAndFlush(account);
        events.publishUserRegistered(new UserRegisteredEvent(account.getId(), email, request.firstName(), request.lastName()));
        return AccountView.from(account);
    }

    @Transactional
    public AccountView replaceRoles(long id, Set<Role> roles) {
        accounts.lockRoleChanges();
        var account = require(id);
        if (account.getRoles().contains(Role.ADMIN) && !roles.contains(Role.ADMIN) && accounts.countWithRole(Role.ADMIN) <= 1) {
            throw conflict("The last administrator cannot be removed");
        }
        account.setRoles(new HashSet<>(roles));
        return AccountView.from(account);
    }

    @Transactional
    public AccountView bootstrap(String supplied, long id) {
        if (bootstrapKey.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Administrator bootstrap is disabled");
        if (supplied == null || !MessageDigest.isEqual(bootstrapKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        accounts.lockRoleChanges();
        if (accounts.countWithRole(Role.ADMIN) > 0) throw conflict("An administrator already exists");
        var account = require(id);
        if (!account.isActive()) throw conflict("Account is inactive");
        account.getRoles().add(Role.ADMIN);
        return AccountView.from(account);
    }

    private static ResponseStatusException conflict(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }
}
