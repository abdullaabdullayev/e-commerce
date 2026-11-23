package com.example.userservice.service;

import com.example.userservice.dto.ProfileDtos.*;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProfileService {
    private final UserRepository users;
    private final String serviceKey;

    public ProfileService(UserRepository users, @Value("${profiles.service-key}") String serviceKey) {
        this.users = users;
        this.serviceKey = serviceKey;
    }

    public Profile get(long id) { return Profile.from(require(id)); }

    @Transactional
    public Profile updateGuardian(long id, Guardian guardian) {
        User user = require(id);
        user.setGuardianName(guardian.name());
        user.setGuardianContact(guardian.contact());
        return Profile.from(user);
    }

    public List<Profile> lookup(String supplied, List<Long> accountIds) {
        if (serviceKey.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        if (supplied == null || !MessageDigest.isEqual(serviceKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return users.findByAccountIdIn(accountIds).stream().map(Profile::from).toList();
    }

    private User require(long id) {
        return users.findByAccountId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile is not ready yet"));
    }
}
