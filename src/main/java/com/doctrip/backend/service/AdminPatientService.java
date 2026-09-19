package com.doctrip.backend.service;

import com.doctrip.backend.dto.AdminPatientResponse;
import com.doctrip.backend.repository.PatientRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminPatientService {

    private final PatientRepository patientRepository;

    public AdminPatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<AdminPatientResponse> list() {
        return patientRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminPatientResponse::from)
                .toList();
    }
}
