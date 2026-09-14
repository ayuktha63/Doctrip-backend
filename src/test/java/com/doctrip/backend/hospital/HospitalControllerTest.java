package com.doctrip.backend.hospital;

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

@WebMvcTest(HospitalController.class)
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HospitalService hospitalService;

    @Test
    void returnsApprovedHospitalsWhenAvailable() throws Exception {
        given(hospitalService.getPubliclyDiscoverableHospitals())
                .willReturn(List.of(new HospitalSummaryDto(1L, "City Hospital", "Multi-specialty hospital")));

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
}
