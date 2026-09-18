package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.HospitalOperationalSettings;
import java.util.UUID;

public record OperationalSettingsResponse(
        UUID hospitalId,
        String sla,
        String coordinationWindow,
        String escalationContact,
        String languagesSupported,
        String minLeadTime,
        String interpreterAvailability) {

    public static OperationalSettingsResponse from(UUID hospitalId, HospitalOperationalSettings s) {
        if (s == null) {
            return new OperationalSettingsResponse(hospitalId, null, null, null, null, null, null);
        }
        return new OperationalSettingsResponse(
                hospitalId, s.getSla(), s.getCoordinationWindow(), s.getEscalationContact(),
                s.getLanguagesSupported(), s.getMinLeadTime(), s.getInterpreterAvailability());
    }
}
