package completo.projeto.completo.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
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
    private Integer oxygenSaturation;    // SpO2 (%)
    private Integer systolicPressure;    // mmHg
    private Integer diastolicPressure;   // mmHg
    private Double temperature;          // °C
    private Integer respiratoryRate;     // rpm
    private String status;               // NORMAL / ALERT

    private LocalDateTime timestamp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientCpf() {
        return patientCpf;
    }

    public void setPatientCpf(String patientCpf) {
        this.patientCpf = patientCpf;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(Integer oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public Integer getSystolicPressure() {
        return systolicPressure;
    }

    public void setSystolicPressure(Integer systolicPressure) {
        this.systolicPressure = systolicPressure;
    }

    public Integer getDiastolicPressure() {
        return diastolicPressure;
    }

    public void setDiastolicPressure(Integer diastolicPressure) {
        this.diastolicPressure = diastolicPressure;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // date/time of reading
}
