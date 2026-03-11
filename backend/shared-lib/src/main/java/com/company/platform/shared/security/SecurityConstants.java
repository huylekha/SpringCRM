package com.company.platform.shared.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CLAIM_USER_ID = "user_id";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_CLAIMS = "claims";
}
