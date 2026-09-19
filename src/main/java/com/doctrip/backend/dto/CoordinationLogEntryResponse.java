package com.doctrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** One human-readable entry in a request's coordination timeline (mockup's "Coordination log" card). */
@Getter
@AllArgsConstructor
@Builder
public class CoordinationLogEntryResponse {
    private String title;
    private String time;
    private String body;
}
