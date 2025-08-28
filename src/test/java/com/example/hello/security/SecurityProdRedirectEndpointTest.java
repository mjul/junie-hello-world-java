package com.example.hello.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityProdRedirectEndpointTest {

  @Autowired MockMvc mockMvc;

  @Test
  void defaultRedirectionEndpointRedirectsToLoginOnError() throws Exception {
    mockMvc
        .perform(get("/login/oauth2/code/github"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("/login?error")));
  }
}
