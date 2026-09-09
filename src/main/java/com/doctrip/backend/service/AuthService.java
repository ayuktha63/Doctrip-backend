package com.doctrip.backend.service;

import com.doctrip.backend.dto.AuthResponse;
import com.doctrip.backend.dto.OtpResendRequest;
import com.doctrip.backend.dto.OtpVerifyRequest;
import com.doctrip.backend.dto.PatientResponse;
import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.dto.RegisterResponse;
import com.doctrip.backend.entity.Patient;
import com.doctrip.backend.exception.BadRequestException;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.PatientRepository;
import com.doctrip.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String PURPOSE_REGISTER = "REGISTER";

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuditService auditService;
    private final JwtService jwtService;

    public AuthService(PatientRepository patientRepository,
                        PasswordEncoder passwordEncoder,
                        OtpService otpService,
                        AuditService auditService,
                        JwtService jwtService) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.auditService = auditService;
        this.jwtService = jwtService;
    }

    /** FR-01 — validates, prevents duplicate identities, creates a pending-verification patient, sends the registration OTP. */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
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

        return new RegisterResponse(patient.getId(), "EMAIL", patient.getEmail(), 300);
    }

    /** FR-03 — successful OTP verification completes registration and establishes an authenticated session. */
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found."));

        otpService.verify(patient.getId(), request.getPurpose(), request.getOtp());

        if ("REGISTER".equalsIgnoreCase(request.getPurpose())) {
            patient.setEmailVerified(true);
            patient.setStatus("active");
            patientRepository.save(patient);
            auditService.record(patient.getId(), "registration_verified", "EMAIL");
        }

        String token = jwtService.generateToken(patient.getId());
        return new AuthResponse(token, PatientResponse.from(patient));
    }

    /** FR-02 resend, subject to the cooldown/rate-limit rules enforced inside OtpService. */
    @Transactional
    public void resendOtp(OtpResendRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found."));
        String channel = "EMAIL";
        String target = patient.getEmail();
        otpService.issue(patient.getId(), request.getPurpose(), channel, target);
    }
}
