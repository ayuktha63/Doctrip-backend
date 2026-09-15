package com.doctrip.backend.dto;

public record HospitalSummaryResponse(
        String hospitalCode, String name, String type, String city, String state, int beds, String accreditation) {}
