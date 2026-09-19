package com.doctrip.backend.department;

import com.doctrip.backend.doctor.DoctorSummaryDto;
import com.doctrip.backend.hospital.HospitalSummaryDto;
import com.doctrip.backend.treatmentpackage.TreatmentPackageSummaryDto;
import java.math.BigDecimal;
import java.util.List;

public record DepartmentDetailDto(
        String name,
        int treatmentCount,
        int doctorCount,
        List<String> hospitalNames,
        BigDecimal fromPrice,
        List<TreatmentPackageSummaryDto> treatments,
        List<DoctorSummaryDto> doctors,
        List<HospitalSummaryDto> hospitals) {}
