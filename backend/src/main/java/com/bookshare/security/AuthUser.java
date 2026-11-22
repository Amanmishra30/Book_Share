package com.bookshare.security;

import java.security.Principal;

/** The authenticated user as carried by the JWT (also used as the STOMP principal). */
public record AuthUser(Long id, String email, String role) implements Principal {
    @Override
    public String getName() { return String.valueOf(id); }

    public boolean isAdmin() { return "ADMIN".equals(role); }
}
