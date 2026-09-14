package com.doctrip.backend.doctor;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @Test
    void returnsApprovedDoctorsWhenAvailable() throws Exception {
        given(doctorService.getPubliclyDiscoverableDoctors())
                .willReturn(List.of(new DoctorSummaryDto(1L, "Dr. Asha Rao", "Orthopedic consultant")));

        mockMvc.perform(get("/api/public/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Dr. Asha Rao"));
    }

    @Test
    void returnsEmptyListWhenNoDoctorsAvailable() throws Exception {
        given(doctorService.getPubliclyDiscoverableDoctors()).willReturn(List.of());

        mockMvc.perform(get("/api/public/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsMatchingDoctorsWhenSearchingByName() throws Exception {
        given(doctorService.searchPubliclyDiscoverableDoctors("Asha"))
                .willReturn(List.of(new DoctorSummaryDto(1L, "Dr. Asha Rao", "Orthopedic consultant")));

        mockMvc.perform(get("/api/public/doctors").param("name", "Asha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Dr. Asha Rao"));
    }

    @Test
    void returnsEmptyListWhenSearchHasNoMatches() throws Exception {
        given(doctorService.searchPubliclyDiscoverableDoctors("Nonexistent")).willReturn(List.of());

        mockMvc.perform(get("/api/public/doctors").param("name", "Nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
