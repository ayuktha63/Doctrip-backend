package com.doctrip.backend.service;

import com.doctrip.backend.doctor.Doctor;
import com.doctrip.backend.doctor.DoctorRepository;
import com.doctrip.backend.dto.hospitaladmin.DoctorRequest;
import com.doctrip.backend.dto.hospitaladmin.DoctorResponse;
import com.doctrip.backend.entity.CoordinationDoctor;
import com.doctrip.backend.entity.Department;
import com.doctrip.backend.entity.HospitalDoctor;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.hospital.DiscoveryHospital;
import com.doctrip.backend.hospital.DiscoveryHospitalRepository;
import com.doctrip.backend.repository.CoordinationDoctorRepository;
import com.doctrip.backend.repository.DepartmentRepository;
import com.doctrip.backend.repository.HospitalDoctorRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalDoctorService {

    private final HospitalDoctorRepository hospitalDoctorRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalAuditService hospitalAuditService;
    private final DiscoveryHospitalRepository discoveryHospitalRepository;
    private final DoctorRepository discoveryDoctorRepository;
    private final CoordinationDoctorRepository coordinationDoctorRepository;

    public HospitalDoctorService(
            HospitalDoctorRepository hospitalDoctorRepository,
            DepartmentRepository departmentRepository,
            HospitalAuditService hospitalAuditService,
            DiscoveryHospitalRepository discoveryHospitalRepository,
            DoctorRepository discoveryDoctorRepository,
            CoordinationDoctorRepository coordinationDoctorRepository) {
        this.hospitalDoctorRepository = hospitalDoctorRepository;
        this.departmentRepository = departmentRepository;
        this.hospitalAuditService = hospitalAuditService;
        this.discoveryHospitalRepository = discoveryHospitalRepository;
        this.discoveryDoctorRepository = discoveryDoctorRepository;
        this.coordinationDoctorRepository = coordinationDoctorRepository;
    }

    public List<DoctorResponse> list(UUID hospitalId) {
        List<HospitalDoctor> doctors = hospitalDoctorRepository.findByHospitalId(hospitalId);
        Map<UUID, String> departmentNames = departmentRepository.findByHospitalId(hospitalId).stream()
                .collect(java.util.stream.Collectors.toMap(Department::getId, Department::getName));
        return doctors.stream()
                .map(d -> DoctorResponse.from(d, departmentNames.get(d.getDepartmentId())))
                .toList();
    }

    @Transactional
    public DoctorResponse create(UUID hospitalId, UUID actorId, DoctorRequest request) {
        Department department = findDepartmentScoped(hospitalId, request.departmentId());
        if (hospitalDoctorRepository.existsByHospitalIdAndNameIgnoreCase(hospitalId, request.name())) {
            throw new ConflictException("A doctor with this name already exists.");
        }

        HospitalDoctor doctor = HospitalDoctor.builder()
                .hospitalId(hospitalId)
                .departmentId(request.departmentId())
                .name(request.name().trim())
                .specialty(request.specialty())
                .experienceYears(request.experienceYears())
                .email(request.email())
                .build();
        doctor = hospitalDoctorRepository.save(doctor);
        syncDiscoveryDoctor(hospitalId, doctor, department.getName());
        syncCoordinationDoctor(hospitalId, doctor, department.getName());

        DoctorResponse after = DoctorResponse.from(doctor, department.getName());
        hospitalAuditService.record(hospitalId, actorId, "DOCTOR", doctor.getId(), "CREATE", null, after);
        return after;
    }

    @Transactional
    public DoctorResponse update(UUID hospitalId, UUID actorId, UUID id, DoctorRequest request) {
        HospitalDoctor doctor = findScoped(hospitalId, id);
        Department department = findDepartmentScoped(hospitalId, request.departmentId());

        if (hospitalDoctorRepository.existsByHospitalIdAndNameIgnoreCaseAndIdNot(hospitalId, request.name(), id)) {
            throw new ConflictException("A doctor with this name already exists.");
        }

        String previousDepartmentName = departmentRepository
                .findById(doctor.getDepartmentId())
                .map(Department::getName)
                .orElse(null);
        DoctorResponse before = DoctorResponse.from(doctor, previousDepartmentName);

        doctor.setName(request.name().trim());
        doctor.setDepartmentId(request.departmentId());
        doctor.setSpecialty(request.specialty());
        doctor.setExperienceYears(request.experienceYears());
        doctor.setEmail(request.email());
        doctor = hospitalDoctorRepository.save(doctor);
        syncDiscoveryDoctor(hospitalId, doctor, department.getName());
        syncCoordinationDoctor(hospitalId, doctor, department.getName());

        DoctorResponse after = DoctorResponse.from(doctor, department.getName());
        hospitalAuditService.record(hospitalId, actorId, "DOCTOR", id, "UPDATE", before, after);
        return after;
    }

    @Transactional
    public DoctorResponse setStatus(UUID hospitalId, UUID actorId, UUID id, boolean active) {
        HospitalDoctor doctor = findScoped(hospitalId, id);
        String departmentName = departmentRepository
                .findById(doctor.getDepartmentId())
                .map(Department::getName)
                .orElse(null);
        DoctorResponse before = DoctorResponse.from(doctor, departmentName);

        doctor.setActive(active);
        doctor = hospitalDoctorRepository.save(doctor);
        syncDiscoveryDoctor(hospitalId, doctor, departmentName);
        syncCoordinationDoctor(hospitalId, doctor, departmentName);

        DoctorResponse after = DoctorResponse.from(doctor, departmentName);
        hospitalAuditService.record(hospitalId, actorId, "DOCTOR", id, "STATUS_TOGGLE", before, after);
        return after;
    }

    /**
     * Keeps the Story 2 public-discovery side (`doctors` / doctor.Doctor, what
     * `/api/public/doctors` actually reads) in sync with this admin console's doctor records —
     * same pattern as HospitalService.syncDiscoveryHospital. `approved` mirrors `active`; a
     * discovery row is only created once the doctor's hospital itself has a linked
     * DiscoveryHospital (i.e. the hospital has been synced — see syncDiscoveryHospital), since a
     * doctor can't be publicly discoverable at a hospital that isn't.
     */
    private void syncDiscoveryDoctor(UUID hospitalId, HospitalDoctor doctor, String departmentName) {
        DiscoveryHospital discoveryHospital = discoveryHospitalRepository
                .findByAdminHospitalId(hospitalId)
                .orElse(null);
        if (discoveryHospital == null) {
            return;
        }
        Doctor discoveryDoctor = discoveryDoctorRepository
                .findByHospitalDoctorId(doctor.getId())
                .orElseGet(Doctor::new);
        discoveryDoctor.setHospitalDoctorId(doctor.getId());
        discoveryDoctor.setHospital(discoveryHospital);
        discoveryDoctor.setName(doctor.getName());
        discoveryDoctor.setDepartment(departmentName);
        discoveryDoctor.setSpecialty(doctor.getSpecialty());
        discoveryDoctor.setExperienceYears(doctor.getExperienceYears());
        discoveryDoctor.setApproved(doctor.isActive());
        discoveryDoctorRepository.save(discoveryDoctor);
    }

    /**
     * Keeps Story 6/7's coordination-eligibility side (`coordination_doctor` /
     * CoordinationDoctor, what AppointmentRequestService.getDoctorAvailability actually checks
     * before "Start coordination" is enabled) in sync with this admin console's doctor records.
     * Without this, a real doctor added via Hospital Admin was never eligible for any request —
     * eligibleDoctors was always empty and the button stayed disabled, regardless of hospital,
     * department, or how many real doctors existed. `available` mirrors `active`.
     */
    private void syncCoordinationDoctor(UUID hospitalId, HospitalDoctor doctor, String departmentName) {
        CoordinationDoctor coordinationDoctor = coordinationDoctorRepository
                .findByHospitalDoctorId(doctor.getId())
                .orElseGet(() -> {
                    CoordinationDoctor fresh = new CoordinationDoctor();
                    fresh.setId(UUID.randomUUID());
                    return fresh;
                });
        coordinationDoctor.setHospitalDoctorId(doctor.getId());
        coordinationDoctor.setHospitalId(hospitalId);
        coordinationDoctor.setName(doctor.getName());
        coordinationDoctor.setDepartment(departmentName);
        coordinationDoctor.setSpecialty(doctor.getSpecialty());
        coordinationDoctor.setEmail(doctor.getEmail());
        coordinationDoctor.setAvailable(doctor.isActive());
        coordinationDoctorRepository.save(coordinationDoctor);
    }

    private HospitalDoctor findScoped(UUID hospitalId, UUID id) {
        HospitalDoctor doctor =
                hospitalDoctorRepository.findById(id).orElseThrow(() -> new NotFoundException("Doctor not found."));
        if (!doctor.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Doctor not found.");
        }
        return doctor;
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
}
