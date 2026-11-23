package com.example.userservice.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Component
public class FamilyAccountDirectory {
    private final RestClient client;
    private record Account(boolean active, Set<String> roles) {}

    FamilyAccountDirectory(@Value("${AUTH_SERVICE_URL:http://localhost:8085}") String authUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(authUrl).requestFactory(factory).build();
    }

    public void requireRole(String token, long id, String role) {
        try {
            var account = client.get().uri("/api/v1/admin/accounts/{id}", id)
                    .headers(headers -> headers.setBearerAuth(token)).retrieve().body(Account.class);
            if (account == null || !account.active() || !account.roles().contains(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected an active " + role + " account");
            }
        } catch (HttpClientErrorException error) {
            throw new ResponseStatusException(error.getStatusCode(), "Account verification failed");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable");
        }
    }
}
