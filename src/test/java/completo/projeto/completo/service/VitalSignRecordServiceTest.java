package completo.projeto.completo.service;

import completo.projeto.completo.VitalSignRecordController;
import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.entities.VitalSignRecord;
import completo.projeto.completo.repository.VitalSignRecordRepository;
import completo.projeto.completo.security.CryptoUtil;
import completo.projeto.completo.security.JwtAuthenticationFilter;
import completo.projeto.completo.websocket.VitalSignWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = VitalSignRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class VitalSignRecordServiceTest {

    @Mock
    private VitalSignRecordRepository repository;

    @MockBean
    private JwtAuthenticationFilter JwtAuthenticationFilter;

    @Mock
    private CryptoUtil cryptoUtil;

    @Mock
    private VitalSignWebSocketService webSocketService;

    @InjectMocks
    private VitalSignRecordService vitalSignRecordService;

    private VitalSignRecordDTO validDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        validDto = new VitalSignRecordDTO(
                "patientId1", "John Doe", "12345678901", 72, 98.5, 120.0, 80.0, 36.5, 16.0, "NORMAL", LocalDateTime.now().toString()
        );
    }

    @Test
    void create_shouldThrowExceptionWhenPatientIdIsMissing() {
        VitalSignRecordDTO invalidDto = new VitalSignRecordDTO(
                "", "John Doe", "12345678901", 72, 98.5, 120.0, 80.0, 36.5, 16.0, "NORMAL", LocalDateTime.now().toString()
        );

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
            vitalSignRecordService.create(invalidDto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        assertEquals("patientId é obrigatório", thrown.getReason());
    }


    @Test
    void getLatestByPatientId_shouldReturnLatestRecords() {
        VitalSignRecord record = new VitalSignRecord();
        record.setPatientId(validDto.patientId());
        record.setHeartRate(validDto.heartRate());
        record.setOxygenSaturation(validDto.oxygenSaturation());
        record.setSystolicPressure(validDto.systolicPressure());
        record.setDiastolicPressure(validDto.diastolicPressure());
        record.setTemperature(validDto.temperature());
        record.setRespiratoryRate(validDto.respiratoryRate());
        record.setStatus(validDto.status());
        record.setTimestamp(LocalDateTime.now());

        when(repository.findTop10ByPatientIdOrderByTimestampDesc(validDto.patientId())).thenReturn(List.of(record));

        var records = vitalSignRecordService.getLatestByPatientId(validDto.patientId());

        assertEquals(1, records.size());
        assertEquals(validDto.patientId(), records.get(0).patientId());
    }

    @Test
    void resolveTimestamp_shouldThrowExceptionForInvalidTimestamp() {
        String invalidTimestamp = "invalid-timestamp";

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
            vitalSignRecordService.resolveTimestamp(invalidTimestamp);
        });

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        assertEquals("timestamp em formato inválido (use ISO-8601)", thrown.getReason());
    }

    @Test
    void validateFirstRecordRequireds_shouldThrowExceptionIfPatientNameIsMissingOnFirstRecord() {
        VitalSignRecord entity = new VitalSignRecord();
        entity.setPatientId(validDto.patientId());
        entity.setPatientName("");
        entity.setPatientCpf(validDto.patientCpf());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
            vitalSignRecordService.validateFirstRecordRequireds(entity, true);
        });

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        assertEquals("patientName é obrigatório no primeiro registro do paciente " + validDto.patientId(), thrown.getReason());
    }
}
