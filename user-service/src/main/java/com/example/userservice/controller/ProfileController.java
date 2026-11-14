package com.example.userservice.controller;

import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@Validated
public class ProfileController {
    private final UserRepository users;
    private final String serviceKey;
    public ProfileController(UserRepository users,@Value("${profiles.service-key}") String serviceKey) { this.users=users; this.serviceKey=serviceKey; }
    public record Profile(long accountId,String firstName,String lastName,String guardianName,String guardianContact) {}
    public record Lookup(@NotEmpty @Size(max=50) List<@NotNull @Positive Long> accountIds) {}
    public record Guardian(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=200) String contact) {}
    private Profile view(User u) { return new Profile(u.getAccountId(),u.getFirstName(),u.getLastName(),u.getGuardianName(),u.getGuardianContact()); }
    private User require(long id) { return users.findByAccountId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile is not ready yet")); }
    @GetMapping("/api/v1/users/me") public Profile me(@AuthenticationPrincipal Jwt jwt) { return view(require(((Number)jwt.getClaim("accountId")).longValue())); }
    @GetMapping("/api/v1/users/{accountId}") public Profile get(@PathVariable @Positive long accountId) { return view(require(accountId)); }
    @PutMapping("/api/v1/users/{accountId}/guardian") @Transactional public Profile guardian(@PathVariable @Positive long accountId,@Valid @RequestBody Guardian r) { User u=require(accountId); u.setGuardianName(r.name()); u.setGuardianContact(r.contact()); return view(users.save(u)); }
    @PostMapping("/internal/v1/users/lookup") public List<Profile> lookup(@RequestHeader(value="X-Profile-Service-Key",required=false) String supplied,@Valid @RequestBody Lookup r) {
        if(serviceKey.length()<32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        if(supplied==null || !MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return users.findByAccountIdIn(r.accountIds()).stream().map(this::view).toList();
    }
}
