package com.example.courseservice.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.Set;
import static com.example.courseservice.exception.DomainException.*;

@Component
public class AuthAccountClient {
    private final RestClient client;
    private record AccountInfo(long accountId, boolean active, Set<String> roles) {}

    public AuthAccountClient(@Value("${courses.auth-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public void requireRole(String token, long accountId, String role) {
        try {
            AccountInfo account = client.get().uri("/api/v1/auth/accounts/{id}", accountId)
                    .headers(headers -> headers.setBearerAuth(token)).retrieve().body(AccountInfo.class);
            if (account == null || !account.active() || account.roles() == null || !account.roles().contains(role)) {
                throw badRequest("Account must be active and have role " + role);
            }
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 404) throw badRequest("Account does not exist");
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) throw forbidden();
            throw exception;
        }
    }
}
