package com.doctrip.backend.service;

import com.doctrip.backend.dto.PatientResponse;
import com.doctrip.backend.entity.Patient;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public PatientResponse getById(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found."));
        return PatientResponse.from(patient);
    }
}
