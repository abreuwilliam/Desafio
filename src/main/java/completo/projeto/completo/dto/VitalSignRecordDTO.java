package completo.projeto.completo.dto;

import java.time.LocalDateTime;

public record VitalSignRecordDTO(
        String patientId,
        String patientName,
        String patientCpf,
        Integer heartRate,
        Double oxygenSaturation,
        Double systolicPressure,
        Double diastolicPressure,
        Double temperature,
        Double respiratoryRate,
        String status,
        String timestamp
) {}
