package com.example.hello.security;

/**
 * Normalized profile information from an external OAuth2/OIDC provider.
 */
public record ProviderProfile(
        String provider,      // e.g. "AZURE" or "GITHUB"
        String externalId,    // stable external id (e.g., sub/id)
        String username,      // login/userPrincipalName/mailNickname
        String displayName,   // best-effort human name
        String email,         // may be null
        String avatarUrl      // may be null
) {
}
