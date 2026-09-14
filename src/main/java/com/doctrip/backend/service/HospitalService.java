package com.doctrip.backend.service;

import com.doctrip.backend.dto.HospitalRequest;
import com.doctrip.backend.dto.HospitalResponse;
import com.doctrip.backend.dto.HospitalSummaryResponse;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.HospitalRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
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

        return toResponse(hospitalRepository.save(hospital));
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

        return toResponse(hospitalRepository.save(hospital));
    }

    @Transactional
    public HospitalResponse setActive(UUID id, boolean active) {
        Hospital hospital = findOrThrow(id);
        hospital.setActive(active);
        return toResponse(hospitalRepository.save(hospital));
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
