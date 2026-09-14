package com.doctrip.backend.treatmentpackage;

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

import com.doctrip.backend.common.ResourceNotFoundException;

@WebMvcTest(TreatmentPackageController.class)
class TreatmentPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentPackageService treatmentPackageService;

    @Test
    void returnsApprovedPackagesWhenAvailable() throws Exception {
        given(treatmentPackageService.getPubliclyDiscoverablePackages())
                .willReturn(List.of(new TreatmentPackageSummaryDto(1L, "Knee Replacement", "Full package")));

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
                .willReturn(new TreatmentPackageSummaryDto(1L, "Knee Replacement", "Full package"));

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
