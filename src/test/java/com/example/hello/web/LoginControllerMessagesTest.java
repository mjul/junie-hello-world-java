package com.example.hello.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerMessagesTest {

  @Autowired MockMvc mockMvc;

  @Test
  void showsLogoutMessageWhenParamPresent() throws Exception {
    mockMvc
        .perform(get("/login?logout"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("You have been logged out.")));
  }

  @Test
  void showsErrorMessageWhenParamPresent() throws Exception {
    mockMvc
        .perform(get("/login?error"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Authentication error")));
  }
}
