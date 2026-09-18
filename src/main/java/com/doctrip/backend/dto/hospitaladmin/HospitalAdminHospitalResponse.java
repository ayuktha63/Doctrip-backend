package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.Hospital;
import java.util.UUID;

public record HospitalAdminHospitalResponse(
        UUID id,
        String hospitalCode,
        String name,
        String type,
        String city,
        String state,
        String accreditation,
        int beds) {

    public static HospitalAdminHospitalResponse from(Hospital h) {
        return new HospitalAdminHospitalResponse(
                h.getId(), h.getHospitalCode(), h.getName(), h.getType(), h.getCity(), h.getState(),
                h.getAccreditation(), h.getBeds());
    }
}
