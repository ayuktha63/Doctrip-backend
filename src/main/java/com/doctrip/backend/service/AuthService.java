package com.doctrip.backend.service;

import com.doctrip.backend.dto.AuthResponse;
import com.doctrip.backend.dto.LoginOtpRequest;
import com.doctrip.backend.dto.LoginRequest;
import com.doctrip.backend.dto.OAuthCompleteRequest;
import com.doctrip.backend.dto.OtpChallengeResponse;
import com.doctrip.backend.dto.OtpResendRequest;
import com.doctrip.backend.dto.OtpVerifyRequest;
import com.doctrip.backend.dto.PatientResponse;
import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.entity.ExternalIdentity;
import com.doctrip.backend.entity.Patient;
import com.doctrip.backend.exception.BadRequestException;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.exception.TooManyRequestsException;
import com.doctrip.backend.exception.UnauthorizedException;
import com.doctrip.backend.repository.ExternalIdentityRepository;
import com.doctrip.backend.repository.PatientRepository;
import com.doctrip.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final String PURPOSE_LOGIN = "LOGIN";

    private final PatientRepository patientRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuditService auditService;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(PatientRepository patientRepository,
                        ExternalIdentityRepository externalIdentityRepository,
                        PasswordEncoder passwordEncoder,
                        OtpService otpService,
                        AuditService auditService,
                        JwtService jwtService,
                        LoginRateLimiter loginRateLimiter) {
        this.patientRepository = patientRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.auditService = auditService;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
    }

    /** FR-01 — validates, prevents duplicate identities, creates a pending-verification patient, sends the registration OTP. */
    @Transactional
    public OtpChallengeResponse register(RegisterRequest request) {
        if (!PasswordPolicy.isValid(request.getPassword())) {
            throw new BadRequestException(
                    "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.");
        }
        // BR-08 — prevent unintended duplicate patient accounts on either supported identity field.
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An account already exists for this email.");
        }
        if (patientRepository.existsByMobile(request.getMobile())) {
            throw new ConflictException("An account already exists for this mobile number.");
        }

        Patient patient = Patient.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("pending_verification")
                .build();
        patient = patientRepository.save(patient);
        auditService.record(patient.getId(), "registration_created", "EMAIL");

        // FR-02 verification channel: email and/or mobile, at least one — email is the default here.
        otpService.issue(patient.getId(), PURPOSE_REGISTER, "EMAIL", patient.getEmail());

        return new OtpChallengeResponse(patient.getId(), "EMAIL", patient.getEmail(), 300);
    }

    /** FR-03/FR-05 — successful OTP verification completes registration (or login) and establishes an authenticated session. */
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found."));

        otpService.verify(patient.getId(), request.getPurpose(), request.getOtp());

        if (PURPOSE_REGISTER.equalsIgnoreCase(request.getPurpose())) {
            patient.setEmailVerified(true);
            patient.setStatus("active");
            patientRepository.save(patient);
            auditService.record(patient.getId(), "registration_verified", "EMAIL");
        } else if (PURPOSE_LOGIN.equalsIgnoreCase(request.getPurpose())) {
            // Login-via-OTP never touches loginRateLimiter's failure counter (that's the password
            // path's own brute-force guard, keyed by whatever identifier string the user typed),
            // so there's nothing to reset here.
            auditService.record(patient.getId(), "login_succeeded_otp", "EMAIL");
        }

        String token = jwtService.generateToken(patient.getId());
        return new AuthResponse(token, PatientResponse.from(patient));
    }

    /** FR-02/FR-05 resend, subject to the cooldown/rate-limit rules enforced inside OtpService. */
    @Transactional
    public void resendOtp(OtpResendRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found."));
        otpService.issue(patient.getId(), request.getPurpose(), "EMAIL", patient.getEmail());
    }

    /** FR-04 — password login. Locks out after repeated failures (BR-06) via a Redis-backed rate limiter. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getLoginId().trim();
        if (loginRateLimiter.isLocked(identifier)) {
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
        }

        Patient patient = patientRepository.findByEmailOrMobile(identifier).orElse(null);
        if (patient == null || patient.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), patient.getPasswordHash())) {
            loginRateLimiter.recordFailure(identifier);
            auditService.record(patient != null ? patient.getId() : null, "login_failed", "PASSWORD");
            throw new UnauthorizedException("Email/mobile or password is incorrect.");
        }
        if (!"active".equals(patient.getStatus())) {
            throw new UnauthorizedException("Please complete registration verification before logging in.");
        }

        loginRateLimiter.reset(identifier);
        auditService.record(patient.getId(), "login_succeeded", "PASSWORD");
        String token = jwtService.generateToken(patient.getId());
        return new AuthResponse(token, PatientResponse.from(patient));
    }

    /** FR-05 — issues a login OTP for a patient identified by email or mobile. */
    @Transactional
    public OtpChallengeResponse requestLoginOtp(LoginOtpRequest request) {
        Patient patient = patientRepository.findByEmailOrMobile(request.getLoginId().trim())
                .orElseThrow(() -> new NotFoundException("No account found for this email or mobile."));

        otpService.issue(patient.getId(), PURPOSE_LOGIN, "EMAIL", patient.getEmail());
        return new OtpChallengeResponse(patient.getId(), "EMAIL", patient.getEmail(), 300);
    }

    /**
     * FR-06/FR-07 — resolves or creates a patient from a completed Google/Apple OAuth2 handshake.
     * Only ever called with claims Spring Security already validated (see OAuthCompleteRequest's
     * javadoc) — never from a public, client-supplied request body.
     */
    @Transactional
    public AuthResponse completeOAuth(OAuthCompleteRequest req) {
        String provider = req.getProvider().toLowerCase();

        // Account-linking safety (BR-08 applied to OAuth): linking to an existing account purely by
        // email match is only safe if the provider vouches the email is verified. Only block when a
        // provider explicitly says "false" — an absent claim means "provider doesn't tell us", not
        // "unverified" (matches HRMS-backend's OAuth2LoginSuccessHandler reasoning for the same case).
        if (Boolean.FALSE.equals(req.getEmailVerified())) {
            throw new UnauthorizedException("Your email address is not verified with " + provider + ".");
        }

        Optional<ExternalIdentity> existingIdentity =
                externalIdentityRepository.findByProviderAndExternalId(provider, req.getExternalId());

        Patient patient;
        if (existingIdentity.isPresent()) {
            patient = patientRepository.findById(existingIdentity.get().getPatientId())
                    .orElseThrow(() -> new UnauthorizedException("Authentication failed. Please try again."));
        } else {
            Optional<Patient> byEmail = patientRepository.findByEmail(req.getEmail());
            if (byEmail.isPresent()) {
                patient = byEmail.get();
                if (!patient.getAuthProviders().contains(provider)) {
                    patient.getAuthProviders().add(provider);
                    patient = patientRepository.save(patient);
                }
            } else {
                patient = Patient.builder()
                        .fullName(req.getName())
                        .email(req.getEmail())
                        .passwordHash(null)
                        .emailVerified(true)
                        .status("active")
                        .build();
                patient.getAuthProviders().add(provider);
                patient = patientRepository.save(patient);
                auditService.record(patient.getId(), "registration_created_oauth", provider.toUpperCase());
            }
            externalIdentityRepository.save(ExternalIdentity.builder()
                    .provider(provider)
                    .externalId(req.getExternalId())
                    .patientId(patient.getId())
                    .build());
        }

        auditService.record(patient.getId(), "oauth_login_succeeded", provider.toUpperCase());
        String token = jwtService.generateToken(patient.getId());
        return new AuthResponse(token, PatientResponse.from(patient));
    }
}
