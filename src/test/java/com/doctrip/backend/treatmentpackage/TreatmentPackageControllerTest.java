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

@WebMvcTest(TreatmentPackageController.class)
class TreatmentPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentPackageService treatmentPackageService;

    @Test
    void returnsApprovedActivePackagesWhenAvailable() throws Exception {
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
}
