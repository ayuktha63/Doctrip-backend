package com.doctrip.backend.hospital;

import java.util.List;

import org.springframework.stereotype.Service;

import com.doctrip.backend.common.ResourceNotFoundException;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public List<HospitalSummaryDto> getPubliclyDiscoverableHospitals() {
        return hospitalRepository.findByApprovedTrue()
                .stream()
                .map(HospitalSummaryDto::from)
                .toList();
    }

    public List<HospitalSummaryDto> searchPubliclyDiscoverableHospitals(String name) {
        return hospitalRepository.findByApprovedTrueAndNameContainingIgnoreCase(name)
                .stream()
                .map(HospitalSummaryDto::from)
                .toList();
    }

    public HospitalSummaryDto getPubliclyDiscoverableHospitalById(Long id) {
        return hospitalRepository.findByIdAndApprovedTrue(id)
                .map(HospitalSummaryDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
    }
}
