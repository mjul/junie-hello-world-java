package com.example.hello.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void mePageRendersForAuthenticatedGithubUser() throws Exception {
        mockMvc.perform(get("/me").with(oauth2Login().attributes(attrs -> {
                    attrs.put("id", "123");
                    attrs.put("login", "octo");
                    attrs.put("name", "Octo Cat");
                    attrs.put("email", "octo@example.com");
                    attrs.put("avatar_url", "http://example.com/avatar.png");
                })))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello, Octo Cat!")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
}
