package com.example.hello.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Global error handling to provide a friendly error page and safe fallbacks. */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(OAuth2AuthenticationException.class)
  public String handleOAuth2AuthError(OAuth2AuthenticationException ex) {
    // Redirect the user back to login with a generic error indicator
    log.debug("OAuth2AuthenticationException: {}", ex.getMessage());
    return "redirect:/login?error";
  }

  @ExceptionHandler(Exception.class)
  public String handleGenericError(Exception ex, Model model) {
    // Log details server-side; render a friendly error page for the user
    log.error("Unhandled exception in MVC layer", ex);
    model.addAttribute("message", "Something went wrong. Please try again.");
    return "error";
  }
}
