package com.example.hello.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("dev")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityDevRedirectEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void devRedirectionEndpointBaseUriIsActive() throws Exception {
        // When the redirection endpoint base URI is /auth/callback/* in dev,
        // accessing the callback path without required params should trigger auth failure
        // and redirect to /login?error (handled by the default failure handler).
        mockMvc.perform(get("/auth/callback/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login?error")));
    }
}
