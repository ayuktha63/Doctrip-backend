package com.doctrip.backend.security;

import com.doctrip.backend.dto.AuthResponse;
import com.doctrip.backend.dto.OAuthCompleteRequest;
import com.doctrip.backend.dto.PatientResponse;
import com.doctrip.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for a real production bug: calling firstNonBlank(Object...) with a single
 * argument whose static type resolves to plain Object (OAuth2User.getAttribute's generic return
 * type) is the classic Java varargs pitfall — javac passes the argument through as the array
 * itself instead of wrapping it, and it blows up with a ClassCastException at runtime the moment
 * a real value (a String) flows through. OAuthFlowTests never caught this because it calls
 * AuthService.completeOAuth directly, never exercising this handler's own claim-extraction logic.
 */
class OAuth2LoginSuccessHandlerTest {

    @Test
    void extractsClaimsAndRedirectsWithTokensOnRealOAuth2User() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.completeOAuth(any(OAuthCompleteRequest.class))).thenReturn(
                new AuthResponse("access-token-123", "refresh-token-456", new PatientResponse()));

        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(authService);
        ReflectionTestUtils.setField(handler, "frontendRedirectUri", "http://localhost:4200/auth/callback");

        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-sub-1", "email", "person@example.com", "name", "Person Example", "email_verified", true),
                "sub");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken(oAuth2User, null, List.of()));

        ArgumentCaptor<OAuthCompleteRequest> captor = ArgumentCaptor.forClass(OAuthCompleteRequest.class);
        verify(authService).completeOAuth(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo("google");
        assertThat(captor.getValue().getEmail()).isEqualTo("person@example.com");
        assertThat(captor.getValue().getExternalId()).isEqualTo("google-sub-1");

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        assertThat(redirectCaptor.getValue())
                .startsWith("http://localhost:4200/auth/callback?token=access-token-123")
                .contains("refreshToken=refresh-token-456");
    }
}
