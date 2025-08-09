package completo.projeto.completo.controller;

import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.service.VitalSignRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        VitalSignRecordDTO saved = service.create(in);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/patient/{id}/latest")
    public List<VitalSignRecordDTO> getLatestByPatient(@PathVariable("id") String patientId) {
        return service.getLatestByPatientId(patientId);
    }

    @GetMapping("/patient/{id}/history")
    public List<VitalSignRecordDTO> getHistoryByPatient(@PathVariable("id") String patientId) {
        return service.getHistoryByPatientId(patientId);
    }

    @GetMapping("/latest")
    public List<VitalSignRecordDTO> getLatestAll(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return service.getLatestAll(limit);
    }

    @GetMapping("/history")
    public List<VitalSignRecordDTO> getHistoryAll() {
        return service.getHistoryAll();
    }
}
