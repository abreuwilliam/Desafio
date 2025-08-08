package completo.projeto.completo.service;

import completo.projeto.completo.Autenticacao.CryptoUtil;
import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.entities.VitalSignRecord;
import completo.projeto.completo.repository.VitalSignRecordRepository;
import completo.projeto.completo.websocket.VitalSignWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VitalSignRecordService {

    @Autowired
    private VitalSignRecordRepository repository;

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private VitalSignWebSocketService webSocketService;

    public VitalSignRecord save(VitalSignRecord record) {
        System.out.println("[SERVICE] Salvando registro: " + record.getPatientId());
        // criptografa dados sensíveis antes de persistir
        if (record.getPatientCpf() != null) {
            record.setPatientCpf(cryptoUtil.encrypt(record.getPatientCpf()));
        }
        if (record.getPatientName() != null) {
            record.setPatientName(cryptoUtil.encrypt(record.getPatientName()));
        }

        VitalSignRecord saved = repository.save(record);

        // emite em tempo real via WebSocket
        VitalSignRecordDTO dto = toDTO(saved);
        webSocketService.sendToDashboard(dto);
        webSocketService.sendToPatient(dto.patientId(), dto);

        return saved;
    }

    // ---------- POR PACIENTE ----------
    public List<VitalSignRecordDTO> getLatestByPatientId(String patientId) {
        return repository.findTop10ByPatientIdOrderByTimestampDesc(patientId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<VitalSignRecordDTO> getHistoryByPatientId(String patientId) {
        return repository.findByPatientId(patientId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ---------- GLOBAIS (SEM FILTRO) ----------
    public List<VitalSignRecordDTO> getLatestAll(int limit) {
        return repository
                .findAllByOrderByTimestampDesc(PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<VitalSignRecordDTO> getHistoryAll() {
        return repository
                .findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // conversão entidade -> DTO (sem expor dados sensíveis)
    private VitalSignRecordDTO toDTO(VitalSignRecord r) {
        return new VitalSignRecordDTO(
                r.getPatientId(),
                r.getHeartRate(),
                r.getOxygenSaturation(),
                r.getSystolicPressure(),
                r.getDiastolicPressure(),
                r.getTemperature(),
                r.getRespiratoryRate(),
                r.getStatus(),
                r.getTimestamp()
        );
    }
}
