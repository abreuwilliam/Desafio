package completo.projeto.completo.dto;

import java.time.LocalDateTime;

public record VitalSignRecordDTO(
        String patientId,
        String patientName,
        String patientCpf,
        Integer heartRate,
        Integer oxygenSaturation,
        Integer systolicPressure,
        Integer diastolicPressure,
        Double temperature,
        Integer respiratoryRate,
        String status,
        String timestamp
) {}
