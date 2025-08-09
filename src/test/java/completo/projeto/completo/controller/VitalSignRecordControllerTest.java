package completo.projeto.completo.controller;


import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.service.VitalSignRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VitalSignRecordController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class VitalSignRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VitalSignRecordService service;

    private VitalSignRecordDTO sampleDto() {
        return new VitalSignRecordDTO(
                "PAC001",
                "João Silva",
                null,           // LGPD: controller responde sem CPF
                87,             // HR
                96,             // SpO2
                130,            // SYS
                85,             // DIA
                36.7,           // temp
                18,             // resp
                "NORMAL",
                "2025-08-09T12:00:00"
        );
    }

    @Test
    @DisplayName("POST /api/v1/vital-signs -> 201 CREATED")
    void testCreate() throws Exception {
        when(service.create(ArgumentMatchers.any(VitalSignRecordDTO.class)))
                .thenReturn(sampleDto());

        String body = """
            {
              "patientId":"PAC001",
              "patientName":"João Silva",
              "patientCpf":"123.456.789-00",
              "heartRate":87,
              "oxygenSaturation":96,
              "systolicPressure":130,
              "diastolicPressure":85,
              "temperature":36.7,
              "respiratoryRate":18,
              "status":"NORMAL",
              "timestamp":"2025-08-09T12:00:00"
            }
            """;

        mockMvc.perform(post("/api/v1/vital-signs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value("PAC001"))
                .andExpect(jsonPath("$.patientName").value("João Silva"))
                .andExpect(jsonPath("$.patientCpf").doesNotExist()); // seguro
    }

    @Test
    @DisplayName("GET /api/v1/vital-signs/patient/{id}/latest -> 200 OK")
    void testGetLatestByPatient() throws Exception {
        when(service.getLatestByPatientId("PAC001"))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/v1/vital-signs/patient/PAC001/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].patientId").value("PAC001"));
    }

    @Test
    @DisplayName("GET /api/v1/vital-signs/patient/{id}/history -> 200 OK")
    void testGetHistoryByPatient() throws Exception {
        when(service.getHistoryByPatientId("PAC001"))
                .thenReturn(List.of(sampleDto(), sampleDto()));

        mockMvc.perform(get("/api/v1/vital-signs/patient/PAC001/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/vital-signs/latest?limit=5 -> 200 OK")
    void testGetLatestAll() throws Exception {
        when(service.getLatestAll(5))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/v1/vital-signs/latest?limit=5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/v1/vital-signs/history -> 200 OK")
    void testGetHistoryAll() throws Exception {
        when(service.getHistoryAll())
                .thenReturn(List.of(sampleDto(), sampleDto(), sampleDto()));

        mockMvc.perform(get("/api/v1/vital-signs/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3));
    }
}

