package com.doctrip.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * FR-06 (the Doctor's own story owns the real UI for this) — the three shapes a doctor's response
 * can take. {@code action} is one of {@code ACCEPT}, {@code REJECT}, {@code PROPOSE_ALTERNATIVE};
 * {@code proposedDate}/{@code proposedTime} are required only for {@code PROPOSE_ALTERNATIVE}.
 */
public record DoctorResponseRequest(String action, LocalDate proposedDate, LocalTime proposedTime) {}
