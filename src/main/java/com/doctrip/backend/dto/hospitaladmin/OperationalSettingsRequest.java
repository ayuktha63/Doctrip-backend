package com.doctrip.backend.dto.hospitaladmin;

public record OperationalSettingsRequest(
        String sla,
        String coordinationWindow,
        String escalationContact,
        String languagesSupported,
        String minLeadTime,
        String interpreterAvailability) {}
