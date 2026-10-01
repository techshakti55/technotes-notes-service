package com.technotes.notes.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public String getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Authenticated JWT principal is required."
            );
        }

        Jwt jwt = jwtAuthentication.getToken();

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException(
                    "JWT subject claim is required."
            );
        }

        return subject;
    }
}