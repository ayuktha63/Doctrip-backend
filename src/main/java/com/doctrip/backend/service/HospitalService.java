package com.doctrip.backend.service;

import com.doctrip.backend.dto.HospitalRequest;
import com.doctrip.backend.dto.HospitalResponse;
import com.doctrip.backend.dto.HospitalSummaryResponse;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.hospital.DiscoveryHospital;
import com.doctrip.backend.hospital.DiscoveryHospitalRepository;
import com.doctrip.backend.repository.HospitalRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalService {

    /**
     * No DocTrip platform-admin authentication exists yet (see CLAUDE.md's Story 3 section and
     * the TODO on /api/admin/** in SecurityConfig) — there is no real actor identity to record.
     * Used only until that story adds one; every row audited under this id is understood to mean
     * "some caller of the still-unauthenticated /api/admin/hospitals/** API."
     */
    private static final UUID PLATFORM_ADMIN_ACTOR_ID = new UUID(0L, 0L);
    private static final String ACTOR_TYPE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String ENTITY_TYPE_HOSPITAL = "HOSPITAL";

    private final HospitalRepository hospitalRepository;
    private final DiscoveryHospitalRepository discoveryHospitalRepository;
    private final HospitalAuditService hospitalAuditService;

    public HospitalService(
            HospitalRepository hospitalRepository,
            DiscoveryHospitalRepository discoveryHospitalRepository,
            HospitalAuditService hospitalAuditService) {
        this.hospitalRepository = hospitalRepository;
        this.discoveryHospitalRepository = discoveryHospitalRepository;
        this.hospitalAuditService = hospitalAuditService;
    }

    @Transactional
    public HospitalResponse create(HospitalRequest request) {
        if (hospitalRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.name(), request.city())) {
            throw new ConflictException("A hospital with this name already exists in this city.");
        }

        Hospital hospital = Hospital.builder()
                .hospitalCode(nextHospitalCode())
                .name(request.name().trim())
                .type(blankToDefault(request.type(), "Multi-speciality"))
                .city(request.city().trim())
                .state(blankToDefault(request.state(), "Kerala"))
                .registrationNumber(request.registrationNumber())
                .accreditation(request.accreditation())
                .beds(request.beds())
                .contactPerson(request.contactPerson())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .build();

        Hospital saved = hospitalRepository.save(hospital);
        syncDiscoveryHospital(saved);
        HospitalResponse after = toResponse(saved);
        audit(saved.getId(), "CREATE", null, after);
        return after;
    }

    public HospitalResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public List<HospitalResponse> list() {
        return hospitalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public HospitalResponse update(UUID id, HospitalRequest request) {
        Hospital hospital = findOrThrow(id);

        hospitalRepository
                .findByNameIgnoreCaseAndCityIgnoreCase(request.name(), request.city())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A hospital with this name already exists in this city.");
                });

        HospitalResponse before = toResponse(hospital);

        hospital.setName(request.name().trim());
        hospital.setType(blankToDefault(request.type(), "Multi-speciality"));
        hospital.setCity(request.city().trim());
        hospital.setState(blankToDefault(request.state(), "Kerala"));
        hospital.setRegistrationNumber(request.registrationNumber());
        hospital.setAccreditation(request.accreditation());
        hospital.setBeds(request.beds());
        hospital.setContactPerson(request.contactPerson());
        hospital.setContactEmail(request.contactEmail());
        hospital.setContactPhone(request.contactPhone());

        Hospital saved = hospitalRepository.save(hospital);
        syncDiscoveryHospital(saved);
        HospitalResponse after = toResponse(saved);
        audit(saved.getId(), "UPDATE", before, after);
        return after;
    }

    @Transactional
    public HospitalResponse setActive(UUID id, boolean active) {
        Hospital hospital = findOrThrow(id);
        HospitalResponse before = toResponse(hospital);

        hospital.setActive(active);
        Hospital saved = hospitalRepository.save(hospital);
        syncDiscoveryHospital(saved);
        HospitalResponse after = toResponse(saved);
        audit(saved.getId(), "STATUS_TOGGLE", before, after);
        return after;
    }

    private void audit(UUID hospitalId, String action, Object previousValue, Object newValue) {
        hospitalAuditService.record(
                hospitalId, ACTOR_TYPE_PLATFORM_ADMIN, PLATFORM_ADMIN_ACTOR_ID, ENTITY_TYPE_HOSPITAL, hospitalId,
                action, previousValue, newValue);
    }

    /**
     * Keeps the Story 2 public-discovery side (`hospitals` / DiscoveryHospital, what
     * `/api/public/hospitals` actually reads) in sync with this admin console's hospital
     * records. Without this, a hospital created/activated here never appears in patient-facing
     * discovery — the two features shipped against separate tables and were never wired
     * together. `approved` mirrors `active`: only hospitals the admin has activated should be
     * publicly discoverable.
     */
    private void syncDiscoveryHospital(Hospital hospital) {
        DiscoveryHospital discoveryHospital = discoveryHospitalRepository
                .findByAdminHospitalId(hospital.getId())
                .orElseGet(DiscoveryHospital::new);
        discoveryHospital.setAdminHospitalId(hospital.getId());
        discoveryHospital.setName(hospital.getName());
        discoveryHospital.setType(hospital.getType());
        discoveryHospital.setCity(hospital.getCity());
        discoveryHospital.setBeds(hospital.getBeds());
        discoveryHospital.setAccreditation(hospital.getAccreditation());
        discoveryHospital.setApproved(hospital.isActive());
        discoveryHospitalRepository.save(discoveryHospital);
    }

    public List<HospitalSummaryResponse> discover() {
        return hospitalRepository.findByActiveTrue().stream()
                .map(h -> new HospitalSummaryResponse(
                        h.getHospitalCode(), h.getName(), h.getType(), h.getCity(), h.getState(), h.getBeds(), h
                                .getAccreditation()))
                .toList();
    }

    private Hospital findOrThrow(UUID id) {
        return hospitalRepository.findById(id).orElseThrow(() -> new NotFoundException("Hospital not found."));
    }

    private String nextHospitalCode() {
        long seq = hospitalRepository.nextHospitalCodeSequence();
        return "HSP-%04d".formatted(seq);
    }

    private String blankToDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private HospitalResponse toResponse(Hospital h) {
        return new HospitalResponse(
                h.getId(),
                h.getHospitalCode(),
                h.getName(),
                h.getType(),
                h.getCity(),
                h.getState(),
                h.getRegistrationNumber(),
                h.getAccreditation(),
                h.getBeds(),
                h.getContactPerson(),
                h.getContactEmail(),
                h.getContactPhone(),
                h.isActive(),
                h.getCreatedAt(),
                h.getUpdatedAt());
    }
}
