package com.doctrip.backend.department;

import com.doctrip.backend.common.ResourceNotFoundException;
import com.doctrip.backend.doctor.Doctor;
import com.doctrip.backend.doctor.DoctorRepository;
import com.doctrip.backend.doctor.DoctorSummaryDto;
import com.doctrip.backend.hospital.DiscoveryHospital;
import com.doctrip.backend.hospital.HospitalSummaryDto;
import com.doctrip.backend.treatmentpackage.TreatmentPackage;
import com.doctrip.backend.treatmentpackage.TreatmentPackageRepository;
import com.doctrip.backend.treatmentpackage.TreatmentPackageSummaryDto;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates "Departments" for public discovery purely from Story 2's own already-reliable
 * fields ({@link Doctor#getDepartment()}/{@link TreatmentPackage#getDepartment()}) rather than
 * Story 4's separate {@code Department} entity, which is keyed against the real {@code Hospital}
 * (UUID) rather than {@link DiscoveryHospital} (Long) — joining across that only-partially-synced
 * bridge (see backend CLAUDE.md's {@code DiscoveryHospital.adminHospitalId}) isn't necessary when
 * every approved doctor/treatment already carries the department name directly. A department
 * "exists" here exactly when there's real, approved content under it.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentDiscoveryService {

    private final DoctorRepository doctorRepository;
    private final TreatmentPackageRepository treatmentPackageRepository;

    public DepartmentDiscoveryService(
            DoctorRepository doctorRepository, TreatmentPackageRepository treatmentPackageRepository) {
        this.doctorRepository = doctorRepository;
        this.treatmentPackageRepository = treatmentPackageRepository;
    }

    public List<DepartmentSummaryDto> listDepartments() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(doctorRepository.findDistinctDepartments());
        names.addAll(treatmentPackageRepository.findDistinctDepartments());

        return names.stream()
                .sorted(Comparator.naturalOrder())
                .map(this::summarize)
                .toList();
    }

    public DepartmentDetailDto getDepartmentByName(String name) {
        List<Doctor> doctors = doctorRepository.findByApprovedTrueAndDepartment(name);
        List<TreatmentPackage> treatments = treatmentPackageRepository.findByApprovedTrueAndDepartment(name);

        if (doctors.isEmpty() && treatments.isEmpty()) {
            throw new ResourceNotFoundException("Department not found");
        }

        Map<Long, DiscoveryHospital> hospitalsById = new LinkedHashMap<>();
        doctors.stream().map(Doctor::getHospital).filter(java.util.Objects::nonNull)
                .forEach(h -> hospitalsById.putIfAbsent(h.getId(), h));
        treatments.stream().map(TreatmentPackage::getHospital).filter(java.util.Objects::nonNull)
                .forEach(h -> hospitalsById.putIfAbsent(h.getId(), h));

        List<HospitalSummaryDto> hospitals = hospitalsById.values().stream()
                .map(HospitalSummaryDto::from)
                .toList();
        // Deduplicated by name, not id — two distinct hospital rows sharing a name (real,
        // pre-existing seed-data duplication) shouldn't repeat the same name in this display list;
        // `hospitals` above intentionally keeps them as separate full records.
        List<String> hospitalNames = new LinkedHashSet<>(
                hospitals.stream().map(HospitalSummaryDto::name).toList()).stream().toList();
        java.math.BigDecimal fromPrice = treatments.stream()
                .map(TreatmentPackage::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.naturalOrder())
                .orElse(null);

        return new DepartmentDetailDto(
                name,
                treatments.size(),
                doctors.size(),
                hospitalNames,
                fromPrice,
                treatments.stream().map(TreatmentPackageSummaryDto::from).toList(),
                doctors.stream().map(DoctorSummaryDto::from).toList(),
                hospitals);
    }

    private DepartmentSummaryDto summarize(String name) {
        List<Doctor> doctors = doctorRepository.findByApprovedTrueAndDepartment(name);
        List<TreatmentPackage> treatments = treatmentPackageRepository.findByApprovedTrueAndDepartment(name);

        Set<String> hospitalNames = new LinkedHashSet<>();
        doctors.stream()
                .map(Doctor::getHospital)
                .filter(java.util.Objects::nonNull)
                .map(DiscoveryHospital::getName)
                .forEach(hospitalNames::add);
        treatments.stream()
                .map(TreatmentPackage::getHospital)
                .filter(java.util.Objects::nonNull)
                .map(DiscoveryHospital::getName)
                .forEach(hospitalNames::add);

        java.math.BigDecimal fromPrice = treatments.stream()
                .map(TreatmentPackage::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.naturalOrder())
                .orElse(null);

        return new DepartmentSummaryDto(name, treatments.size(), doctors.size(), List.copyOf(hospitalNames), fromPrice);
    }
}
