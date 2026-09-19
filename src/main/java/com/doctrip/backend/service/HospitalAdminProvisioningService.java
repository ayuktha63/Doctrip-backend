package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.HospitalAdminCreateRequest;
import com.doctrip.backend.dto.hospitaladmin.HospitalAdminResponse;
import com.doctrip.backend.dto.hospitaladmin.HospitalAdminUpdateRequest;
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
 * DocTrip-admin-driven provisioning of Hospital Admin accounts — create, list and update (name /
 * email / password, viewable via CredentialEncryptionService) — no self-registration, no
 * deactivate for this story.
 */
@Service
public class HospitalAdminProvisioningService {

    private final HospitalAdminRepository hospitalAdminRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialEncryptionService credentialEncryptionService;

    public HospitalAdminProvisioningService(
            HospitalAdminRepository hospitalAdminRepository,
            HospitalRepository hospitalRepository,
            PasswordEncoder passwordEncoder,
            CredentialEncryptionService credentialEncryptionService) {
        this.hospitalAdminRepository = hospitalAdminRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.credentialEncryptionService = credentialEncryptionService;
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
                .encryptedPassword(credentialEncryptionService.encrypt(request.password()))
                .name(request.name().trim())
                .build();

        admin = hospitalAdminRepository.save(admin);
        return HospitalAdminResponse.from(admin, request.password());
    }

    public List<HospitalAdminResponse> list(UUID hospitalId) {
        List<HospitalAdmin> admins =
                hospitalId != null
                        ? hospitalAdminRepository.findByHospitalId(hospitalId)
                        : hospitalAdminRepository.findAll();
        return admins.stream().map(this::toResponseWithDecryptedPassword).toList();
    }

    @Transactional
    public HospitalAdminResponse update(UUID id, HospitalAdminUpdateRequest request) {
        HospitalAdmin admin =
                hospitalAdminRepository.findById(id).orElseThrow(() -> new NotFoundException("Hospital admin not found."));

        if (hospitalAdminRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("A hospital admin with this email already exists.");
        }

        admin.setName(request.name().trim());
        admin.setEmail(request.email().trim());
        if (request.password() != null && !request.password().isBlank()) {
            admin.setPasswordHash(passwordEncoder.encode(request.password()));
            admin.setEncryptedPassword(credentialEncryptionService.encrypt(request.password()));
        }

        admin = hospitalAdminRepository.save(admin);
        return toResponseWithDecryptedPassword(admin);
    }

    private HospitalAdminResponse toResponseWithDecryptedPassword(HospitalAdmin admin) {
        // Accounts created before this field existed have no encrypted_password yet — surface as
        // null rather than throwing, so an older admin's row still lists/edits fine (just without
        // a viewable current password until the next password change re-populates it).
        String decrypted = admin.getEncryptedPassword() != null
                ? credentialEncryptionService.decrypt(admin.getEncryptedPassword())
                : null;
        return HospitalAdminResponse.from(admin, decrypted);
    }
}
