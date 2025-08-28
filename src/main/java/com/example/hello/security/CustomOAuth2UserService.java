package com.example.hello.security;

import com.example.hello.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Customizes the user loading to normalize profiles and onboard/update a local User. */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService
    implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

  private final UserService userService;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public CustomOAuth2UserService(UserService userService) {
    this.userService = userService;
    this.restTemplate = new RestTemplate();
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2User oauth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    Map<String, Object> attributes = oauth2User.getAttributes();

    ProviderProfile profile;
    if ("github".equalsIgnoreCase(registrationId)) {
      profile = resolveGithubProfile(attributes, userRequest.getAccessToken());
    } else if ("azure".equalsIgnoreCase(registrationId)) {
      profile = ProviderProfileMapper.fromAzure(attributes);
    } else {
      // Fallback: map minimally from whatever is present
      profile =
          new ProviderProfile(
              registrationId.toUpperCase(),
              string(attributes.get("sub")),
              firstNonBlank(
                  string(attributes.get("preferred_username")), string(attributes.get("name"))),
              string(attributes.get("name")),
              string(attributes.get("email")),
              null);
    }

    // Upsert local user
    userService.getOrCreateFromProviderProfile(profile);

    // Return the original user principal, preserving authorities and name attribute
    String nameAttributeKey =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();
    if (nameAttributeKey == null || nameAttributeKey.isBlank()) {
      nameAttributeKey = "sub"; // safe default
    }
    Collection<? extends GrantedAuthority> authorities = oauth2User.getAuthorities();
    return new DefaultOAuth2User(authorities, attributes, nameAttributeKey);
  }

  private ProviderProfile resolveGithubProfile(
      Map<String, Object> attributes, OAuth2AccessToken token) {
    ProviderProfile base = ProviderProfileMapper.fromGithub(attributes);
    if (base.email() == null
        && token != null
        && token.getScopes() != null
        && token.getScopes().contains("user:email")) {
      try {
        String email = fetchPrimaryVerifiedGithubEmail(token);
        if (email != null) {
          return new ProviderProfile(
              base.provider(),
              base.externalId(),
              base.username(),
              base.displayName(),
              email,
              base.avatarUrl());
        }
      } catch (Exception e) {
        log.debug("GitHub emails fetch failed: {}", e.toString());
      }
    }
    return base;
  }

  private String fetchPrimaryVerifiedGithubEmail(OAuth2AccessToken token)
      throws RestClientException {
    String url = "https://api.github.com/user/emails";
    var headers = new HttpHeaders();
    headers.setBearerAuth(token.getTokenValue());
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    var entity = new org.springframework.http.HttpEntity<Void>(headers);
    var response =
        restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
    String body = response.getBody();
    if (body == null || body.isBlank()) return null;
    try {
      JsonNode arr = objectMapper.readTree(body);
      String primaryVerified = null;
      String anyVerified = null;
      String first = null;
      if (arr.isArray()) {
        for (JsonNode n : arr) {
          String email = n.path("email").asText(null);
          boolean primary = n.path("primary").asBoolean(false);
          boolean verified = n.path("verified").asBoolean(false);
          if (first == null) first = email;
          if (verified && anyVerified == null) anyVerified = email;
          if (primary && verified) {
            primaryVerified = email;
            break;
          }
        }
      }
      if (primaryVerified != null) return primaryVerified;
      if (anyVerified != null) return anyVerified;
      return first;
    } catch (Exception e) {
      log.debug("Failed to parse GitHub emails JSON: {}", e.toString());
      return null;
    }
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
