package com.example.authservice.service;

import com.example.authservice.model.Account;
import com.example.authservice.model.RefreshToken;
import com.example.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public RefreshToken createRefreshToken(Account account){

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .account(account)
                .expiryDate(
                        Instant.now().plusMillis(refreshTokenExpiration)
                )
                .build();
        return  refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken findByToken(String token){
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found!"));
    }

    public RefreshToken verifyExpiration(RefreshToken token){

        if(token.getExpiryDate().isBefore(Instant.now())){
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token is expired!");
        }
        return token;
    }

    public void deleteByToken(String token){
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }



}
