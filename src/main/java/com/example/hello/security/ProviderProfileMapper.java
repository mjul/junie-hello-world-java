package com.example.hello.security;

import java.util.Map;

/** Maps raw provider attributes to a ProviderProfile with consistent semantics. */
public final class ProviderProfileMapper {

  private ProviderProfileMapper() {}

  public static ProviderProfile fromGithub(Map<String, Object> attributes) {
    String provider = "GITHUB";
    String externalId = string(attributes.get("id"));
    String login = string(attributes.get("login"));
    String name = string(attributes.get("name"));
    String email = string(attributes.get("email")); // may be null/private
    String avatarUrl = string(attributes.get("avatar_url"));

    String displayName = firstNonBlank(name, login);
    String username = firstNonBlank(login, externalId);

    return new ProviderProfile(provider, externalId, username, displayName, email, avatarUrl);
  }

  public static ProviderProfile fromAzure(Map<String, Object> attributes) {
    String provider = "AZURE";
    // Common OIDC claims: sub, name, preferred_username, email. Some deployments also expose oid.
    String externalId = firstNonBlank(string(attributes.get("oid")), string(attributes.get("sub")));
    String preferredUsername = string(attributes.get("preferred_username"));
    String upn = string(attributes.get("userPrincipalName"));
    String mailNickname = string(attributes.get("mailNickname"));
    String displayNameClaim = string(attributes.get("name"));
    String email = string(attributes.get("email"));

    String username = firstNonBlank(preferredUsername, upn, mailNickname, email, externalId);
    String displayName = firstNonBlank(displayNameClaim, mailNickname, upn, username);

    return new ProviderProfile(provider, externalId, username, displayName, email, null);
  }

  private static String string(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static String firstNonBlank(String... vals) {
    if (vals == null) return null;
    for (String v : vals) {
      if (v != null && !v.isBlank()) return v;
    }
    return null;
  }
}
