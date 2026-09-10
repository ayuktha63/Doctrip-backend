package com.doctrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Verified identity claims from a completed OAuth2 provider handshake (FR-06 Google, FR-07 Apple).
 * Deliberately NOT a public REST request body — {@link com.doctrip.backend.security.OAuth2LoginSuccessHandler}
 * builds this only from claims Spring Security already validated against the provider's token/JWK
 * endpoint during the real `oauth2Login()` flow. Exposing an endpoint that accepts these fields
 * directly from a client would let anyone self-declare an arbitrary verified email and hijack or
 * create accounts without ever proving they own that identity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthCompleteRequest {
    private String provider;
    private String externalId;
    private String email;
    private String name;

    /** Null when the provider doesn't send this claim at all (treated as "unknown", not "false"). */
    private Boolean emailVerified;
}
