package completo.projeto.completo.controller;



import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.entities.VitalSignRecord;
import completo.projeto.completo.mapper.VitalSignRecordMapper;
import completo.projeto.completo.service.VitalSignRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/api/v1/vital-signs")
public class VitalSignRecordController {

    @Autowired
    private VitalSignRecordService service;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody @Valid VitalSignRecordDTO dto) {
        VitalSignRecord entity = VitalSignRecordMapper.INSTANCE.toEntity(dto);
        service.save(entity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/patient/{id}/latest")
    public List<VitalSignRecordDTO> getLatest(@PathVariable String id) {
        return service.getLatestByPatientId(id);
    }

    @GetMapping("/patient/{id}/history")
    public List<VitalSignRecordDTO> getHistory(@PathVariable String id) {
        return service.getHistoryByPatientId(id);

    }
    @GetMapping("/latest")
    public List<VitalSignRecordDTO> getLatestAll(@RequestParam(defaultValue = "50") int limit) {
        return service.getLatestAll(limit);
    }

    // Histórico completo global (ordenado desc)
    @GetMapping("/history")
    public List<VitalSignRecordDTO> getHistoryAll() {
        return service.getHistoryAll();
    }
}
