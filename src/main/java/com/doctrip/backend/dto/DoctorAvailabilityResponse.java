package com.doctrip.backend.dto;

import java.util.List;

/** FR-02/FR-04/FR-05 — a preview of doctor availability for a request, before coordination starts. */
public record DoctorAvailabilityResponse(
        String requestedDoctorName,
        boolean requestedDoctorAvailable,
        List<DoctorCandidateResponse> eligibleDoctors) {}
