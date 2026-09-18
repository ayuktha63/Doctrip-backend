package com.doctrip.backend.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Authenticated-principal wrapper for a logged-in Hospital Admin, scoped to exactly one hospital. */
@Getter
@AllArgsConstructor
public class HospitalAdminPrincipal {
    private final UUID hospitalAdminId;
    private final UUID hospitalId;
}
