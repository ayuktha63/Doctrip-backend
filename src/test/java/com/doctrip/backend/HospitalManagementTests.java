package com.doctrip.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> hospitalPayload(String name, String city) {
        return Map.of(
                "name", name,
                "city", city,
                "beds", 120,
                "contactEmail", "desk@" + name.toLowerCase().replace(" ", "") + ".example");
    }

    @Test
    void createsHospitalWithGeneratedCodeAndInactiveByDefault() throws Exception {
        mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospitalPayload("Sunrise Hospital", "Kochi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalCode").value(org.hamcrest.Matchers.startsWith("HSP-")))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.type").value("Multi-speciality"))
                .andExpect(jsonPath("$.state").value("Kerala"));
    }

    @Test
    void rejectsCreateWithMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "", "city", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateHospitalInSameCity() throws Exception {
        mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospitalPayload("Lakeside Hospital", "Alappuzha"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospitalPayload("Lakeside Hospital", "Alappuzha"))))
                .andExpect(status().isConflict());
    }

    @Test
    void editsHospitalAndTogglesActivation() throws Exception {
        String createResponse = mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospitalPayload("Riverside Hospital", "Thrissur"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);
        String id = created.get("id").asText();

        Map<String, Object> edit = Map.of(
                "name", "Riverside Multispeciality Hospital",
                "city", "Thrissur",
                "beds", 200,
                "accreditation", "NABH");
        mockMvc.perform(put("/api/admin/hospitals/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Riverside Multispeciality Hospital"))
                .andExpect(jsonPath("$.beds").value(200));

        mockMvc.perform(post("/api/admin/hospitals/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void discoveryOnlyShowsActiveHospitalsWithNarrowShape() throws Exception {
        String createResponse = mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospitalPayload("Coastal Care Hospital", "Kollam"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Coastal Care Hospital')]").doesNotExist());

        mockMvc.perform(post("/api/admin/hospitals/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", true))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Coastal Care Hospital')].registrationNumber").doesNotExist())
                .andExpect(jsonPath("$[?(@.name == 'Coastal Care Hospital')].hospitalCode").exists());
    }

    @Test
    void returnsNotFoundForUnknownHospital() throws Exception {
        mockMvc.perform(get("/api/admin/hospitals/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
