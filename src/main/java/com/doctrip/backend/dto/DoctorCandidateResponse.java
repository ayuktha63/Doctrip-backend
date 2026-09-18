package com.doctrip.backend.dto;

import java.util.UUID;

/** One candidate doctor for a department (FR-05 eligibility). Backed by dummy doctor scaffolding
 * until real Doctor master data exists — see backend CLAUDE.md. */
public record DoctorCandidateResponse(UUID doctorId, String name, String specialty) {}
