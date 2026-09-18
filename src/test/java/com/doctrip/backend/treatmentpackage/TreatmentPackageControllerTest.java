package com.doctrip.backend.treatmentpackage;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.doctrip.backend.security.JwtService;
import com.doctrip.backend.repository.RevokedTokenRepository;
import org.springframework.test.web.servlet.MockMvc;

import com.doctrip.backend.common.ResourceNotFoundException;

@WebMvcTest(TreatmentPackageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TreatmentPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RevokedTokenRepository revokedTokenRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TreatmentPackageService treatmentPackageService;

    @Test
    void returnsApprovedPackagesWhenAvailable() throws Exception {
        given(treatmentPackageService.getPubliclyDiscoverablePackages())
                .willReturn(List.of(new TreatmentPackageSummaryDto(1L, "Knee Replacement", "Full package",
                        "Orthopaedics", new BigDecimal("120000"), "10 days", 1L, "Aster Medcity")));

        mockMvc.perform(get("/api/public/treatment-packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Knee Replacement"));
    }

    @Test
    void returnsEmptyListWhenNoPackagesAvailable() throws Exception {
        given(treatmentPackageService.getPubliclyDiscoverablePackages()).willReturn(List.of());

        mockMvc.perform(get("/api/public/treatment-packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsPackageDetailsWhenApprovedPackageExists() throws Exception {
        given(treatmentPackageService.getPubliclyDiscoverablePackageById(1L))
                .willReturn(new TreatmentPackageDetailDto(1L, "Knee Replacement", "Full package",
                        "Orthopaedics", new BigDecimal("120000"), "10 days", 1L, "Aster Medcity",
                        "Kochi", "NABH · JCI", List.of()));

        mockMvc.perform(get("/api/public/treatment-packages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Knee Replacement"));
    }

    @Test
    void returnsNotFoundWhenPackageDoesNotExistOrIsNotApproved() throws Exception {
        given(treatmentPackageService.getPubliclyDiscoverablePackageById(99L))
                .willThrow(new ResourceNotFoundException("Treatment package not found"));

        mockMvc.perform(get("/api/public/treatment-packages/99"))
                .andExpect(status().isNotFound());
    }
}
