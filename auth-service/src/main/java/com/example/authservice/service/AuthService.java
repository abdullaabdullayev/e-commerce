package com.example.authservice.service;

import com.example.authservice.auth.JwtUtil;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.RegisterResponse;
import com.example.authservice.messaging.UserEventPublisher;
import com.example.authservice.model.Account;
import com.example.authservice.model.Role;
import com.example.authservice.model.RefreshToken;
import com.example.authservice.repository.AccountRepository;
import com.example.user.api.event.UserRegisteredEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
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

    @Transactional
    public RegisterResponse register(RegisterRequest request){
        return createAccount(request, Role.CUSTOMER);
    }

    @Transactional
    public RegisterResponse registerInvitedParent(RegisterRequest request) {
        return createAccount(request, Role.PARENT);
    }

    private RegisterResponse createAccount(RegisterRequest request, Role role) {
        String email = request.getEmail();

        if(accountRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        Account account = new Account();
        account.setRoles(new HashSet<>(Set.of(role)));
        account.setEmail(request.getEmail());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Account savedAccount = accountRepository.save(account);

        userEventPublisher.publishUserRegistered(new UserRegisteredEvent(
                savedAccount.getId(),
                savedAccount.getEmail(),
                request.getFirstName(),
                request.getLastName()
        ));

        return RegisterResponse.builder()
                .accountId(savedAccount.getId())
                .email(savedAccount.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
    }

    public AuthResponse login(LoginRequest request){
        String username = request.getIdentifier();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );

        Account account =accountRepository.findByIdentifier(username)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String accessToken =jwtUtil.generateAccessToken(account);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account);
        return new AuthResponse(accessToken, refreshToken.getToken());


    }
}
