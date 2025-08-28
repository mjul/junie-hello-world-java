package com.example.hello.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ErrorControllerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void errorEndpointRendersFriendlyPage() throws Exception {
    mockMvc
        .perform(get("/error"))
        .andExpect(status().is5xxServerError())
        .andExpect(content().string(containsString("Back to login")))
        .andExpect(content().string(containsString("Oops")));
  }
}
