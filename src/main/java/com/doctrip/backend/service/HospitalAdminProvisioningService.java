package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.HospitalAdminCreateRequest;
import com.doctrip.backend.dto.hospitaladmin.HospitalAdminResponse;
import com.doctrip.backend.entity.HospitalAdmin;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.HospitalAdminRepository;
import com.doctrip.backend.repository.HospitalRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DocTrip-admin-driven provisioning of Hospital Admin accounts — create + list only, no
 * self-registration, no update/deactivate for this story.
 */
@Service
public class HospitalAdminProvisioningService {

    private final HospitalAdminRepository hospitalAdminRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    public HospitalAdminProvisioningService(
            HospitalAdminRepository hospitalAdminRepository,
            HospitalRepository hospitalRepository,
            PasswordEncoder passwordEncoder) {
        this.hospitalAdminRepository = hospitalAdminRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public HospitalAdminResponse create(HospitalAdminCreateRequest request) {
        if (!hospitalRepository.existsById(request.hospitalId())) {
            throw new NotFoundException("Hospital not found.");
        }
        if (hospitalAdminRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A hospital admin with this email already exists.");
        }

        HospitalAdmin admin = HospitalAdmin.builder()
                .hospitalId(request.hospitalId())
                .email(request.email().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .build();

        return HospitalAdminResponse.from(hospitalAdminRepository.save(admin));
    }

    public List<HospitalAdminResponse> list(UUID hospitalId) {
        List<HospitalAdmin> admins =
                hospitalId != null
                        ? hospitalAdminRepository.findByHospitalId(hospitalId)
                        : hospitalAdminRepository.findAll();
        return admins.stream().map(HospitalAdminResponse::from).toList();
    }
}
