package com.example.authservice.service;

import com.example.authservice.auth.JwtUtil;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.RegisterResponse;
import com.example.authservice.messaging.UserEventPublisher;
import com.example.authservice.model.Account;
import com.example.authservice.model.Role;
import com.example.authservice.repository.AccountRepository;
import com.example.user.api.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserEventPublisher userEventPublisher;
    private final LoginAttemptLimiter loginLimiter;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        return createAccount(request, Role.CUSTOMER);
    }

    @Transactional
    public RegisterResponse registerInvitedParent(RegisterRequest request) {
        return createAccount(request, Role.PARENT);
    }

    private RegisterResponse createAccount(RegisterRequest request, Role role) {
        String email = Emails.normalize(request.getEmail());

        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        Account account = new Account();
        account.setRoles(new HashSet<>(Set.of(role)));
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Account savedAccount = accountRepository.save(account);

        userEventPublisher.publishUserRegistered(new UserRegisteredEvent(
                savedAccount.getId(), savedAccount.getEmail(), request.getFirstName(), request.getLastName()));

        return RegisterResponse.builder()
                .accountId(savedAccount.getId())
                .email(savedAccount.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier();
        loginLimiter.check(identifier);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, request.getPassword()));
        } catch (AuthenticationException failure) {
            // An unavailable database is not a wrong password; do not lock the user out for it.
            if (!(failure instanceof InternalAuthenticationServiceException)) loginLimiter.failed(identifier);
            throw failure;
        }
        loginLimiter.succeeded(identifier);

        Account account = accountRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return issueTokens(account);
    }

    /** Exchanges a refresh token for a new pair; the old refresh token stops working. */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        return issueTokens(refreshTokenService.consume(refreshToken));
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse issueTokens(Account account) {
        return new AuthResponse(jwtUtil.generateAccessToken(account), refreshTokenService.issue(account));
    }
}
