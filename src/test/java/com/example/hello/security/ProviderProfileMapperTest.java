package com.example.hello.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ProviderProfileMapperTest {

  @Test
  void githubMapping_prefersNameThenLogin() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("id", 12345);
    attrs.put("login", "octocat");
    attrs.put("name", "Mona Lisa");
    attrs.put("email", null);
    attrs.put("avatar_url", "https://avatars.githubusercontent.com/u/1?v=4");

    ProviderProfile p = ProviderProfileMapper.fromGithub(attrs);
    assertThat(p.provider()).isEqualTo("GITHUB");
    assertThat(p.externalId()).isEqualTo("12345");
    assertThat(p.username()).isEqualTo("octocat");
    assertThat(p.displayName()).isEqualTo("Mona Lisa");
    assertThat(p.email()).isNull();
    assertThat(p.avatarUrl()).contains("avatars.githubusercontent.com");
  }

  @Test
  void githubMapping_fallsBackToLoginWhenNameMissing() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("id", 777);
    attrs.put("login", "no-name");
    attrs.put("name", null);
    ProviderProfile p = ProviderProfileMapper.fromGithub(attrs);
    assertThat(p.displayName()).isEqualTo("no-name");
    assertThat(p.username()).isEqualTo("no-name");
  }

  @Test
  void azureMapping_usesNameResolutionPriority() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("sub", "abc-sub");
    attrs.put("preferred_username", "user@contoso.com");
    attrs.put("mailNickname", "usernick");
    attrs.put("name", "Azure User");
    ProviderProfile p = ProviderProfileMapper.fromAzure(attrs);
    assertThat(p.provider()).isEqualTo("AZURE");
    assertThat(p.externalId()).isEqualTo("abc-sub");
    assertThat(p.username()).isEqualTo("user@contoso.com");
    assertThat(p.displayName()).isEqualTo("Azure User");
  }
}
