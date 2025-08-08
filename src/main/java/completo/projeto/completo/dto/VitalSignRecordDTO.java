package completo.projeto.completo.dto;

import java.time.LocalDateTime;

public record VitalSignRecordDTO(
        String patientId,
        Integer heartRate,
        Integer oxygenSaturation,
        Integer systolicPressure,
        Integer diastolicPressure,
        Double temperature,
        Integer respiratoryRate,
        String status,
        LocalDateTime timestamp
) {}
