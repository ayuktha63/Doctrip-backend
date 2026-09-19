package com.doctrip.backend.department;

import java.math.BigDecimal;
import java.util.List;

public record DepartmentSummaryDto(
        String name,
        int treatmentCount,
        int doctorCount,
        List<String> hospitalNames,
        BigDecimal fromPrice) {}
