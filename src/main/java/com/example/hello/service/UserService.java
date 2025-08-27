package com.example.hello.service;

import com.example.hello.domain.User;
import com.example.hello.repository.UserRepository;
import com.example.hello.security.ProviderProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getOrCreateFromProviderProfile(ProviderProfile profile) {
        var now = Instant.now();
        var existing = userRepository.findByProviderAndExternalId(profile.provider(), profile.externalId());
        if (existing.isPresent()) {
            User u = existing.get();
            boolean changed = false;
            if (!safeEquals(u.getUsername(), profile.username())) { u.setUsername(profile.username()); changed = true; }
            if (!safeEquals(u.getDisplayName(), profile.displayName())) { u.setDisplayName(profile.displayName()); changed = true; }
            if (!safeEquals(u.getEmail(), profile.email())) { u.setEmail(profile.email()); changed = true; }
            if (!safeEquals(u.getAvatarUrl(), profile.avatarUrl())) { u.setAvatarUrl(profile.avatarUrl()); changed = true; }
            u.setLastLoginAt(now);
            if (changed) {
                // preUpdate will handle updatedAt
            }
            return userRepository.save(u);
        }
        // create new
        User u = new User();
        u.setProvider(profile.provider());
        u.setExternalId(profile.externalId());
        u.setUsername(profile.username());
        u.setDisplayName(profile.displayName());
        u.setEmail(profile.email());
        u.setAvatarUrl(profile.avatarUrl());
        u.setLastLoginAt(now);
        return userRepository.save(u);
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
