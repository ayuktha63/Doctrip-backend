package com.doctrip.backend.treatmentpackage;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.doctrip.backend.common.ResourceNotFoundException;
import com.doctrip.backend.doctor.Doctor;
import com.doctrip.backend.doctor.DoctorRepository;

@Service
@Transactional(readOnly = true)
public class TreatmentPackageService {

    private final TreatmentPackageRepository treatmentPackageRepository;
    private final DoctorRepository doctorRepository;

    public TreatmentPackageService(TreatmentPackageRepository treatmentPackageRepository, DoctorRepository doctorRepository) {
        this.treatmentPackageRepository = treatmentPackageRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<TreatmentPackageSummaryDto> getPubliclyDiscoverablePackages() {
        return treatmentPackageRepository.findPubliclyDiscoverable()
                .stream()
                .map(TreatmentPackageSummaryDto::from)
                .toList();
    }

    public TreatmentPackageDetailDto getPubliclyDiscoverablePackageById(Long id) {
        TreatmentPackage treatmentPackage = treatmentPackageRepository.findPubliclyDiscoverableById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment package not found"));
        List<Doctor> team = (treatmentPackage.getHospital() != null && treatmentPackage.getDepartment() != null)
                ? doctorRepository.findPubliclyDiscoverableByHospitalIdAndDepartment(
                        treatmentPackage.getHospital().getId(), treatmentPackage.getDepartment())
                : List.of();
        return TreatmentPackageDetailDto.from(treatmentPackage, team);
    }
}
