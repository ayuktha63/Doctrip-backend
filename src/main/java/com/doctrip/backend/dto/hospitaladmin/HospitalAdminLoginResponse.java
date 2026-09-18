package com.doctrip.backend.dto.hospitaladmin;

import java.util.UUID;

public record HospitalAdminLoginResponse(
        String accessToken, long expiresIn, UUID hospitalId, String hospitalName) {}
