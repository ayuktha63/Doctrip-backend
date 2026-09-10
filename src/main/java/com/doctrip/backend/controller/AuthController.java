package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AuthResponse;
import com.doctrip.backend.dto.LoginOtpRequest;
import com.doctrip.backend.dto.LoginRequest;
import com.doctrip.backend.dto.OtpChallengeResponse;
import com.doctrip.backend.dto.OtpResendRequest;
import com.doctrip.backend.dto.OtpVerifyRequest;
import com.doctrip.backend.dto.RefreshTokenRequest;
import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.security.PatientPrincipal;
import com.doctrip.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<OtpChallengeResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/api/auth/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/api/auth/otp/resend")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody OtpResendRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/api/auth/login/otp")
    public ResponseEntity<OtpChallengeResponse> requestLoginOtp(@Valid @RequestBody LoginOtpRequest request) {
        return ResponseEntity.ok(authService.requestLoginOtp(request));
    }

    @PostMapping("/api/auth/token/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal PatientPrincipal principal,
                                        @RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.substring("Bearer ".length());
        authService.logout(principal.getPatientId(), accessToken);
        return ResponseEntity.noContent().build();
    }
}
