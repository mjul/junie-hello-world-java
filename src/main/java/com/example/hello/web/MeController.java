package com.example.hello.web;

import com.example.hello.domain.User;
import com.example.hello.security.ProviderProfile;
import com.example.hello.security.ProviderProfileMapper;
import com.example.hello.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public String me(Model model,
                     OAuth2AuthenticationToken authentication,
                     @AuthenticationPrincipal OAuth2User principal) {
        if (authentication == null || principal == null) {
            // Should be handled by security, but keep a safe fallback
            return "redirect:/login";
        }
        String registrationId = authentication.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = principal.getAttributes();

        ProviderProfile profile;
        if ("github".equalsIgnoreCase(registrationId)) {
            profile = ProviderProfileMapper.fromGithub(attributes);
        } else if ("azure".equalsIgnoreCase(registrationId)) {
            profile = ProviderProfileMapper.fromAzure(attributes);
        } else {
            // Fallback minimal mapping
            String provider = registrationId == null ? "UNKNOWN" : registrationId.toUpperCase();
            String externalId = firstNonBlank(string(attributes.get("sub")), string(attributes.get("id")));
            String username = firstNonBlank(string(attributes.get("preferred_username")), string(attributes.get("login")), externalId);
            String displayName = firstNonBlank(string(attributes.get("name")), username);
            String email = string(attributes.get("email"));
            String avatarUrl = string(attributes.get("avatar_url"));
            profile = new ProviderProfile(provider, externalId, username, displayName, email, avatarUrl);
        }

        User user = userService.getOrCreateFromProviderProfile(profile);
        String display = firstNonBlank(user.getDisplayName(), user.getUsername());

        model.addAttribute("displayName", display);
        model.addAttribute("avatarUrl", user.getAvatarUrl());
        model.addAttribute("user", user);
        return "me";
    }

    private static String string(Object o) { return o == null ? null : String.valueOf(o); }
    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) { if (v != null && !v.isBlank()) return v; }
        return null;
    }
}
