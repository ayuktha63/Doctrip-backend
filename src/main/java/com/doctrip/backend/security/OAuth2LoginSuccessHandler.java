package com.doctrip.backend.security;

import com.doctrip.backend.dto.AuthResponse;
import com.doctrip.backend.dto.OAuthCompleteRequest;
import com.doctrip.backend.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Completes a real Spring Security OAuth2 Client login (google/apple registrations, see
 * application.yml + .env) by delegating to AuthService.completeOAuth(...), then redirects to the
 * Angular frontend with a short-lived JWT in the query string so the SPA can pick it up.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final AuthService authService;

    @Value("${doctrip.oauth2.frontend-redirect-uri:http://localhost:4200/auth/callback}")
    private String frontendRedirectUri;

    public OAuth2LoginSuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            response.sendRedirect(frontendRedirectUri + "?error=oauth_failed");
            return;
        }

        String registrationId = extractRegistrationId(request);
        String provider = registrationId != null ? registrationId.toLowerCase() : "google";

        String email = firstNonBlank(oAuth2User.getAttribute("email"));
        String name = firstNonBlank(oAuth2User.getAttribute("name"), email);
        String externalId = firstNonBlank(oAuth2User.getAttribute("sub"), oAuth2User.getName());

        if (email == null || externalId == null) {
            log.warn("OAuth2 login for provider {} did not supply email/subject claims", provider);
            response.sendRedirect(frontendRedirectUri + "?error=missing_claims");
            return;
        }

        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        OAuthCompleteRequest req = OAuthCompleteRequest.builder()
                .provider(provider)
                .externalId(externalId)
                .email(email)
                .name(name != null ? name : email)
                .emailVerified(emailVerified)
                .build();

        try {
            AuthResponse authResponse = authService.completeOAuth(req);
            String redirect = frontendRedirectUri
                    + "?token=" + URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8)
                    + "&refreshToken=" + URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8);
            response.sendRedirect(redirect);
        } catch (Exception e) {
            log.warn("OAuth2 login completion failed for provider {}: {}", provider, e.getMessage());
            response.sendRedirect(frontendRedirectUri + "?error=" +
                    URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Sign-in failed.", StandardCharsets.UTF_8));
        }
    }

    private String extractRegistrationId(HttpServletRequest request) {
        // Spring stores the last authorization request under /login/oauth2/code/{registrationId}
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/login/oauth2/code/")) {
            String[] parts = uri.split("/login/oauth2/code/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    @SafeVarargs
    private final String firstNonBlank(Object... values) {
        for (Object v : values) {
            if (v != null && !v.toString().isBlank()) {
                return v.toString();
            }
        }
        return null;
    }
}
