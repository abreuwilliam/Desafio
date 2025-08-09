package completo.projeto.completo.controller;

import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.service.VitalSignRecordService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/vital-signs")
public class VitalSignRecordController {

    private final VitalSignRecordService service;

    public VitalSignRecordController(VitalSignRecordService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VitalSignRecordDTO> create(@Valid @RequestBody VitalSignRecordDTO in) {
        log.info("api=create_vital_signs action=start patientId={}", in.patientId());
        VitalSignRecordDTO saved = service.create(in);
        log.info("api=create_vital_signs action=success patientId={} status=CREATED", saved.patientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/patient/{id}/latest")
    public List<VitalSignRecordDTO> getLatestByPatient(@PathVariable("id") String patientId) {
        log.info("api=get_latest_by_patient action=start patientId={}", patientId);
        List<VitalSignRecordDTO> out = service.getLatestByPatientId(patientId);
        log.info("api=get_latest_by_patient action=success patientId={} count={}", patientId, out.size());
        return out;
    }

    @GetMapping("/patient/{id}/history")
    public List<VitalSignRecordDTO> getHistoryByPatient(@PathVariable("id") String patientId) {
        log.info("api=get_history_by_patient action=start patientId={}", patientId);
        List<VitalSignRecordDTO> out = service.getHistoryByPatientId(patientId);
        log.info("api=get_history_by_patient action=success patientId={} count={}", patientId, out.size());
        return out;
    }

    @GetMapping("/latest")
    public List<VitalSignRecordDTO> getLatestAll(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        log.info("api=get_latest_all action=start limit={}", limit);
        List<VitalSignRecordDTO> out = service.getLatestAll(limit);
        log.info("api=get_latest_all action=success limit={} count={}", limit, out.size());
        return out;
    }

    @GetMapping("/history")
    public List<VitalSignRecordDTO> getHistoryAll() {
        log.info("api=get_history_all action=start");
        List<VitalSignRecordDTO> out = service.getHistoryAll();
        log.info("api=get_history_all action=success count={}", out.size());
        return out;
    }
}
