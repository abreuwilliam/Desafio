package completo.projeto.completo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class VitalSignRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String patientId;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private String patientCpf;

    private Integer heartRate;           // bpm
    private Double oxygenSaturation;    // SpO2 (%)
    private Double systolicPressure;    // mmHg
    private Double diastolicPressure;   // mmHg
    private Double temperature;          // °C
    private Double respiratoryRate;     // rpm
    private String status;               // NORMAL / ALERT

    private LocalDateTime timestamp;
}
