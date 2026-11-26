package com.example.authservice.service;

import java.util.Locale;

/** Email addresses are compared case-insensitively; new accounts store them in lower case. */
public final class Emails {
    private Emails() {}

    public static String normalize(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
