package com.doctrip.backend.security;

import com.doctrip.backend.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token) && !isRevoked(token)) {
                String role = jwtService.extractRole(token);
                UsernamePasswordAuthenticationToken authentication;
                if ("HOSPITAL_ADMIN".equals(role)) {
                    UUID hospitalAdminId = UUID.fromString(jwtService.extractPatientId(token));
                    UUID hospitalId = UUID.fromString(jwtService.extractHospitalId(token));
                    HospitalAdminPrincipal principal = new HospitalAdminPrincipal(hospitalAdminId, hospitalId);
                    authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_HOSPITAL_ADMIN")));
                } else {
                    String patientId = jwtService.extractPatientId(token);
                    PatientPrincipal principal = new PatientPrincipal(patientId);
                    authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                }
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Rejected request with invalid, expired, or revoked bearer token on {}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRevoked(String token) {
        try {
            String jti = jwtService.extractJti(token);
            return jti != null && revokedTokenRepository.existsByJti(jti);
        } catch (Exception e) {
            return true;
        }
    }
}
