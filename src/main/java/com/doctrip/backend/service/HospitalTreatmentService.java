package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.TreatmentRequest;
import com.doctrip.backend.dto.hospitaladmin.TreatmentResponse;
import com.doctrip.backend.entity.Department;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.hospital.DiscoveryHospital;
import com.doctrip.backend.hospital.DiscoveryHospitalRepository;
import com.doctrip.backend.repository.DepartmentRepository;
import com.doctrip.backend.treatmentpackage.TreatmentPackage;
import com.doctrip.backend.treatmentpackage.TreatmentPackageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hospital-admin CRUD over the existing Story 2 {@link TreatmentPackage} entity, scoped via the
 * real DiscoveryHospital link (HospitalService.syncDiscoveryHospital keeps a DiscoveryHospital row
 * matched 1:1 to this admin's entity.Hospital via DiscoveryHospital.adminHospitalId) — so a
 * treatment created here uses TreatmentPackage's existing `hospital` FK directly and is genuinely
 * visible through the public Treatment Discovery API for this hospital, not a parallel column.
 */
@Service
public class HospitalTreatmentService {

    private final TreatmentPackageRepository treatmentPackageRepository;
    private final DepartmentRepository departmentRepository;
    private final DiscoveryHospitalRepository discoveryHospitalRepository;
    private final HospitalAuditService hospitalAuditService;

    public HospitalTreatmentService(
            TreatmentPackageRepository treatmentPackageRepository,
            DepartmentRepository departmentRepository,
            DiscoveryHospitalRepository discoveryHospitalRepository,
            HospitalAuditService hospitalAuditService) {
        this.treatmentPackageRepository = treatmentPackageRepository;
        this.departmentRepository = departmentRepository;
        this.discoveryHospitalRepository = discoveryHospitalRepository;
        this.hospitalAuditService = hospitalAuditService;
    }

    public List<TreatmentResponse> list(UUID hospitalId) {
        DiscoveryHospital discoveryHospital = findDiscoveryHospital(hospitalId);
        return treatmentPackageRepository.findByHospital_Id(discoveryHospital.getId()).stream()
                .map(t -> TreatmentResponse.from(t, departmentIdFor(hospitalId, t.getDepartment())))
                .toList();
    }

    @Transactional
    public TreatmentResponse create(UUID hospitalId, UUID actorId, TreatmentRequest request) {
        DiscoveryHospital discoveryHospital = findDiscoveryHospital(hospitalId);
        Department department = findDepartmentScoped(hospitalId, request.departmentId());
        if (treatmentPackageRepository.existsByHospital_IdAndNameIgnoreCase(discoveryHospital.getId(), request.name())) {
            throw new ConflictException("A treatment with this name already exists.");
        }

        TreatmentPackage treatment = new TreatmentPackage();
        treatment.setHospital(discoveryHospital);
        treatment.setName(request.name().trim());
        treatment.setDepartment(department.getName());
        treatment.setPrice(request.price());
        treatment.setConsultationFee(request.consultationFee());
        treatment.setTypicalStay(request.typicalStay());
        treatment.setDescription(request.description());
        treatment.setApproved(true);
        treatment = treatmentPackageRepository.save(treatment);

        TreatmentResponse after = TreatmentResponse.from(treatment, department.getId());
        hospitalAuditService.record(
                hospitalId, actorId, "TREATMENT", syntheticId(treatment.getId()), "CREATE", null, after);
        return after;
    }

    @Transactional
    public TreatmentResponse update(UUID hospitalId, UUID actorId, Long id, TreatmentRequest request) {
        DiscoveryHospital discoveryHospital = findDiscoveryHospital(hospitalId);
        TreatmentPackage treatment = findScoped(discoveryHospital, id);
        Department department = findDepartmentScoped(hospitalId, request.departmentId());

        if (treatmentPackageRepository.existsByHospital_IdAndNameIgnoreCaseAndIdNot(
                discoveryHospital.getId(), request.name(), id)) {
            throw new ConflictException("A treatment with this name already exists.");
        }

        TreatmentResponse before =
                TreatmentResponse.from(treatment, departmentIdFor(hospitalId, treatment.getDepartment()));

        treatment.setName(request.name().trim());
        treatment.setDepartment(department.getName());
        treatment.setPrice(request.price());
        treatment.setConsultationFee(request.consultationFee());
        treatment.setTypicalStay(request.typicalStay());
        treatment.setDescription(request.description());
        treatment = treatmentPackageRepository.save(treatment);

        TreatmentResponse after = TreatmentResponse.from(treatment, department.getId());
        hospitalAuditService.record(
                hospitalId, actorId, "TREATMENT", syntheticId(id), "UPDATE", before, after);
        return after;
    }

    @Transactional
    public TreatmentResponse setListed(UUID hospitalId, UUID actorId, Long id, boolean listed) {
        DiscoveryHospital discoveryHospital = findDiscoveryHospital(hospitalId);
        TreatmentPackage treatment = findScoped(discoveryHospital, id);
        UUID departmentId = departmentIdFor(hospitalId, treatment.getDepartment());
        TreatmentResponse before = TreatmentResponse.from(treatment, departmentId);

        treatment.setApproved(listed);
        treatment = treatmentPackageRepository.save(treatment);

        TreatmentResponse after = TreatmentResponse.from(treatment, departmentId);
        hospitalAuditService.record(
                hospitalId, actorId, "TREATMENT", syntheticId(id), "STATUS_TOGGLE", before, after);
        return after;
    }

    /** Hospital-admin ops require an active, already-synced hospital — the sync happens on every
     *  admin create/update/status-change in HospitalService, so this should always resolve once
     *  the hospital-admin's own hospital was created through the normal Story 3 flow. */
    private DiscoveryHospital findDiscoveryHospital(UUID hospitalId) {
        return discoveryHospitalRepository
                .findByAdminHospitalId(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital is not yet available for treatment management."));
    }

    private TreatmentPackage findScoped(DiscoveryHospital discoveryHospital, Long id) {
        TreatmentPackage treatment = treatmentPackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Treatment not found."));
        if (treatment.getHospital() == null || !discoveryHospital.getId().equals(treatment.getHospital().getId())) {
            throw new NotFoundException("Treatment not found.");
        }
        return treatment;
    }

    private Department findDepartmentScoped(UUID hospitalId, UUID departmentId) {
        Department department = departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found."));
        if (!department.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Department not found.");
        }
        return department;
    }

    // TreatmentPackage.department is a plain String (pre-existing Story 2 shape), not a FK, so
    // responses re-resolve it back to a UUID by name lookup within the admin's own hospital.
    // Best-effort: assumes unique department names per hospital (Department creation doesn't
    // itself enforce this beyond the (hospitalId, code) uniqueness).
    private UUID departmentIdFor(UUID hospitalId, String departmentName) {
        if (departmentName == null) {
            return null;
        }
        return departmentRepository.findByHospitalId(hospitalId).stream()
                .filter(d -> d.getName().equalsIgnoreCase(departmentName))
                .map(Department::getId)
                .findFirst()
                .orElse(null);
    }

    // HospitalAuditLog.entityId is UUID but TreatmentPackage.id is Long (kept as-is, no renaming
    // of the existing entity's PK) — derive a deterministic synthetic UUID for the audit row.
    private UUID syntheticId(Long id) {
        return UUID.nameUUIDFromBytes(("treatment-package:" + id).getBytes());
    }
}
