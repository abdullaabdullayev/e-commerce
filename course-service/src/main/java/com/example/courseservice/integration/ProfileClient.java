package com.example.courseservice.integration;

import com.example.courseservice.dto.PageResponse;
import com.example.courseservice.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.*;
import static com.example.courseservice.dto.LearningDtos.*;

@Component
public class ProfileClient {
    private final RestClient client;
    private final String serviceKey;

    public ProfileClient(@Value("${courses.profile-url}") String baseUrl, @Value("${courses.profile-key}") String serviceKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.serviceKey = serviceKey;
    }

    public void requireParentLink(String token, long studentId) {
        try {
            client.get().uri("/internal/v1/parent/children/{id}", studentId)
                    .headers(headers -> headers.setBearerAuth(token)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized denied) {
            throw DomainException.forbidden();
        } catch (RestClientException unavailable) {
            throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "Family service unavailable");
        }
    }

    public RosterResponse enrich(PageResponse<EnrollmentResponse> page) {
        if (page.content().isEmpty()) return new RosterResponse(List.of(), page.page(), page.hasNext(), false);
        Map<Long, Profile> profiles = new HashMap<>();
        boolean unavailable = false;
        try {
            if (serviceKey.isBlank()) throw new IllegalStateException("Profile key is not configured");
            Profile[] response = client.post().uri("/internal/v1/users/lookup")
                    .header("X-Profile-Service-Key", serviceKey)
                    .body(Map.of("accountIds", page.content().stream().map(EnrollmentResponse::studentAccountId).toList()))
                    .retrieve().body(Profile[].class);
            if (response != null) for (Profile profile : response) profiles.put(profile.accountId(), profile);
        } catch (RestClientException | IllegalStateException exception) {
            unavailable = true;
        }
        var content = page.content().stream().map(enrollment -> new RosterEntry(enrollment,
                profiles.get(enrollment.studentAccountId()), !profiles.containsKey(enrollment.studentAccountId()))).toList();
        return new RosterResponse(content, page.page(), page.hasNext(), unavailable);
    }
}
