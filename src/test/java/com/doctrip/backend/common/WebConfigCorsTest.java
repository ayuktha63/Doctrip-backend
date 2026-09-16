package com.doctrip.backend.common;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.doctrip.backend.treatmentpackage.TreatmentPackageController;
import com.doctrip.backend.treatmentpackage.TreatmentPackageService;

@WebMvcTest(TreatmentPackageController.class)
class WebConfigCorsTest {

    private static final String ANGULAR_DEV_ORIGIN = "http://localhost:4300";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentPackageService treatmentPackageService;

    @Test
    void allowsPreflightFromAngularDevOrigin() throws Exception {
        mockMvc.perform(options("/api/public/treatment-packages")
                        .header(HttpHeaders.ORIGIN, ANGULAR_DEV_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_DEV_ORIGIN));
    }

    @Test
    void includesAllowOriginHeaderOnActualGetRequest() throws Exception {
        given(treatmentPackageService.getPubliclyDiscoverablePackages()).willReturn(List.of());

        mockMvc.perform(get("/api/public/treatment-packages")
                        .header(HttpHeaders.ORIGIN, ANGULAR_DEV_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_DEV_ORIGIN));
    }

    @Test
    void rejectsPreflightFromUntrustedOrigin() throws Exception {
        mockMvc.perform(options("/api/public/treatment-packages")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
