package com.doctrip.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Without this, a failed OAuth2 login (provider rejects the request, user cancels, etc.) falls
 * through to Spring Security's default behavior — forwarding to /error, which our JSON
 * AuthenticationEntryPoint then reports as a bare 401, stranding the browser instead of the
 * Angular app. Redirect to the same frontend callback route the success handler uses, with
 * ?error=... instead of ?token=....
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${doctrip.oauth2.frontend-redirect-uri:http://localhost:4200/auth/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 login failed: {}", exception.getMessage());
        String redirect = frontendRedirectUri + "?error=" +
                URLEncoder.encode("Sign-in failed. Please try again.", StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }
}
