package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.HospitalAdminLoginRequest;
import com.doctrip.backend.dto.hospitaladmin.HospitalAdminLoginResponse;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.entity.HospitalAdmin;
import com.doctrip.backend.exception.UnauthorizedException;
import com.doctrip.backend.repository.HospitalAdminRepository;
import com.doctrip.backend.repository.HospitalRepository;
import com.doctrip.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class HospitalAdminAuthService {

    private final HospitalAdminRepository hospitalAdminRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public HospitalAdminAuthService(
            HospitalAdminRepository hospitalAdminRepository,
            HospitalRepository hospitalRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.hospitalAdminRepository = hospitalAdminRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public HospitalAdminLoginResponse login(HospitalAdminLoginRequest request) {
        HospitalAdmin admin = hospitalAdminRepository
                .findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        if (!admin.isActive()) {
            throw new UnauthorizedException("This hospital admin account is deactivated.");
        }

        Hospital hospital = hospitalRepository
                .findById(admin.getHospitalId())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        if (!hospital.isActive()) {
            throw new UnauthorizedException("This hospital is deactivated.");
        }

        String token = jwtService.generateHospitalAdminToken(admin.getId(), admin.getHospitalId());
        return new HospitalAdminLoginResponse(
                token, jwtService.getExpirySeconds(), hospital.getId(), hospital.getName());
    }
}
