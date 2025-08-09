package completo.projeto.completo.service;

import completo.projeto.completo.Autenticacao.CryptoUtil;
import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.entities.VitalSignRecord;
import completo.projeto.completo.repository.VitalSignRecordRepository;
import completo.projeto.completo.websocket.VitalSignWebSocketService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class VitalSignRecordService {

    private static final int MAX_LIMIT = 500;
    private static final int MIN_LIMIT = 1;

    private final VitalSignRecordRepository repository;
    private final CryptoUtil cryptoUtil;
    private final VitalSignWebSocketService webSocketService;

    public VitalSignRecordService(VitalSignRecordRepository repository,
                                  CryptoUtil cryptoUtil,
                                  VitalSignWebSocketService webSocketService) {
        this.repository = repository;
        this.cryptoUtil = cryptoUtil;
        this.webSocketService = webSocketService;
    }

    public VitalSignRecordDTO create(VitalSignRecordDTO in) {
        if (in == null || isBlank(in.patientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId é obrigatório");
        }

        Optional<VitalSignRecord> lastOpt =
                repository.findTopByPatientIdOrderByTimestampDesc(in.patientId());

        VitalSignRecord entity = new VitalSignRecord();
        entity.setPatientId(in.patientId());
        entity.setPatientName(resolvePatientName(in, lastOpt));
        entity.setPatientCpf(resolveEncryptedCpf(in, lastOpt));

        entity.setHeartRate(in.heartRate());
        entity.setOxygenSaturation(in.oxygenSaturation());
        entity.setSystolicPressure(in.systolicPressure());
        entity.setDiastolicPressure(in.diastolicPressure());
        entity.setTemperature(in.temperature());
        entity.setRespiratoryRate(in.respiratoryRate());
        entity.setStatus(in.status());
        entity.setTimestamp(resolveTimestamp(in.timestamp()));

        validateFirstRecordRequireds(entity, lastOpt.isEmpty());

        VitalSignRecord saved = repository.save(entity);

        VitalSignRecordDTO safeDto = toSafeDTO(saved);
        webSocketService.sendToDashboard(safeDto);
        webSocketService.sendToPatient(safeDto.patientId(), safeDto);

        return safeDto;
    }

    public List<VitalSignRecordDTO> getLatestByPatientId(String patientId) {
        return repository.findTop10ByPatientIdOrderByTimestampDesc(patientId)
                .stream()
                .map(this::toSafeDTO)
                .toList();
    }

    public List<VitalSignRecordDTO> getHistoryByPatientId(String patientId) {
        return repository.findByPatientId(patientId)
                .stream()
                .map(this::toSafeDTO)
                .toList();
    }

    public List<VitalSignRecordDTO> getLatestAll(int limit) {
        int safeLimit = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit));
        return repository.findAllByOrderByTimestampDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toSafeDTO)
                .toList();
    }

    public List<VitalSignRecordDTO> getHistoryAll() {
        return repository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toSafeDTO)
                .toList();
    }

    private String resolvePatientName(VitalSignRecordDTO in, Optional<VitalSignRecord> lastOpt) {
        if (!isBlank(in.patientName())) return in.patientName();
        return lastOpt.map(VitalSignRecord::getPatientName).orElse(null);
    }

    private String resolveEncryptedCpf(VitalSignRecordDTO in, Optional<VitalSignRecord> lastOpt) {
        String cpfIn = in.patientCpf();
        if (!isBlank(cpfIn)) {
            String digitsOnly = cpfIn.replaceAll("\\D", "");
            try {
                return cryptoUtil.encrypt(digitsOnly);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao criptografar CPF");
            }
        }
        return lastOpt.map(VitalSignRecord::getPatientCpf).orElse(null);
    }

    private LocalDateTime resolveTimestamp(String timestamp) {
        if (isBlank(timestamp)) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(timestamp);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timestamp em formato inválido (use ISO-8601)");
        }
    }

    private void validateFirstRecordRequireds(VitalSignRecord entity, boolean isFirstRecordForPatient) {
        if (isFirstRecordForPatient) {
            if (isBlank(entity.getPatientName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "patientName é obrigatório no primeiro registro do paciente " + entity.getPatientId()
                );
            }
            if (isBlank(entity.getPatientCpf())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "patientCpf é obrigatório no primeiro registro do paciente " + entity.getPatientId()
                );
            }
        }
    }

    private VitalSignRecordDTO toSafeDTO(VitalSignRecord r) {
        return new VitalSignRecordDTO(
                r.getPatientId(),
                r.getPatientName(),
                r.getPatientCpf(),
                r.getHeartRate(),
                r.getOxygenSaturation(),
                r.getSystolicPressure(),
                r.getDiastolicPressure(),
                r.getTemperature(),
                r.getRespiratoryRate(),
                r.getStatus(),
                r.getTimestamp() != null ? r.getTimestamp().toString() : null
        );
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
