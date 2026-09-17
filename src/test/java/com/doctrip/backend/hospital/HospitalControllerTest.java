package com.doctrip.backend.hospital;

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

@WebMvcTest(HospitalController.class)
@AutoConfigureMockMvc(addFilters = false)
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RevokedTokenRepository revokedTokenRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private DiscoveryHospitalService hospitalService;

    @Test
    void returnsApprovedHospitalsWhenAvailable() throws Exception {
        given(hospitalService.getPubliclyDiscoverableHospitals())
                .willReturn(List.of(new HospitalSummaryDto(1L, "City Hospital", "Multi-specialty hospital", "Multi-speciality", "Kochi", 670, new BigDecimal("4.8"), 512, "NABH · JCI", "English, Malayalam")));

        mockMvc.perform(get("/api/public/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("City Hospital"));
    }

    @Test
    void returnsEmptyListWhenNoHospitalsAvailable() throws Exception {
        given(hospitalService.getPubliclyDiscoverableHospitals()).willReturn(List.of());

        mockMvc.perform(get("/api/public/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsMatchingHospitalsWhenSearchingByName() throws Exception {
        given(hospitalService.searchPubliclyDiscoverableHospitals("City"))
                .willReturn(List.of(new HospitalSummaryDto(1L, "City Hospital", "Multi-specialty hospital", "Multi-speciality", "Kochi", 670, new BigDecimal("4.8"), 512, "NABH · JCI", "English, Malayalam")));

        mockMvc.perform(get("/api/public/hospitals").param("name", "City"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("City Hospital"));
    }

    @Test
    void returnsEmptyListWhenSearchHasNoMatches() throws Exception {
        given(hospitalService.searchPubliclyDiscoverableHospitals("Nonexistent")).willReturn(List.of());

        mockMvc.perform(get("/api/public/hospitals").param("name", "Nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsHospitalProfileWhenApprovedHospitalExists() throws Exception {
        given(hospitalService.getPubliclyDiscoverableHospitalById(1L))
                .willReturn(new HospitalDetailDto(1L, "City Hospital", "Multi-specialty hospital", "Multi-speciality",
                        "Kochi", 670, new BigDecimal("4.8"), 512, "NABH · JCI", "English, Malayalam", 3, List.of()));

        mockMvc.perform(get("/api/public/hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("City Hospital"));
    }

    @Test
    void returnsNotFoundWhenHospitalDoesNotExistOrIsNotApproved() throws Exception {
        given(hospitalService.getPubliclyDiscoverableHospitalById(99L))
                .willThrow(new ResourceNotFoundException("Hospital not found"));

        mockMvc.perform(get("/api/public/hospitals/99"))
                .andExpect(status().isNotFound());
    }
}
