package com.example.hello.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A friendly error controller that renders the error page and ensures a proper 5xx status
 * when the error path is accessed directly. This also avoids exposing internal details
 * and falls back to a generic message.
 */
@Controller
public class FriendlyErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(FriendlyErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = 500;
        try {
            if (statusAttr instanceof Integer i) {
                status = i;
            } else if (statusAttr != null) {
                status = Integer.parseInt(String.valueOf(statusAttr));
            }
        } catch (Exception e) {
            // ignore parse errors; keep default 500
            log.debug("Unable to parse error status: {}", statusAttr);
        }
        // Ensure we do not return a non-error status from /error
        if (status < 400) {
            status = 500;
        }
        response.setStatus(status);

        // A generic, user-friendly message (template also includes fallback text)
        model.addAttribute("message", "Something went wrong. Please try again.");
        return "error";
    }
}
