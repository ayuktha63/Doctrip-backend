package com.doctrip.backend.hospital;

import java.util.List;

import org.springframework.stereotype.Service;

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
}
