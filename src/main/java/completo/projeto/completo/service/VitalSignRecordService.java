package completo.projeto.completo.service;

import completo.projeto.completo.security.CryptoUtil;
import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.entities.VitalSignRecord;
import completo.projeto.completo.repository.VitalSignRecordRepository;
import completo.projeto.completo.websocket.VitalSignWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VitalSignRecordService {

    @Value("${vital-signs.limit.max:500}")
    private int maxLimit;

    @Value("${vital-signs.limit.min:1}")
    private int minLimit;

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
            log.warn("domain=vital_sign create=fail reason=missing_patientId");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId é obrigatório");
        }

        Optional<VitalSignRecord> lastOpt =
                repository.findTopByPatientIdOrderByTimestampDesc(in.patientId());

        VitalSignRecord entity = new VitalSignRecord();
        entity.setPatientId(in.patientId());
        entity.setPatientName(resolvePatientName(in, lastOpt));
        entity.setPatientCpf(resolveEncryptedCpf(in, lastOpt)); // nunca logar CPF

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
        log.info("domain=vital_sign event=save_ok patientId={} recordId={} ts={}",
                saved.getPatientId(), saved.getId(), saved.getTimestamp());

        VitalSignRecordDTO safeDto = toSafeDTO(saved);

        try {
            webSocketService.sendToDashboard(safeDto);
            webSocketService.sendToPatient(safeDto.patientId(), safeDto);
            log.info("domain=vital_sign event=broadcast_ok patientId={} recordId={}",
                    saved.getPatientId(), saved.getId());
        } catch (Exception e) {
            log.error("domain=vital_sign event=broadcast_fail patientId={} recordId={} err='{}'",
                    saved.getPatientId(), saved.getId(), e.getMessage(), e);
        }

        return safeDto;
    }

    public List<VitalSignRecordDTO> getLatestByPatientId(String patientId) {
        log.info("domain=vital_sign query=latest_by_patient start patientId={}", patientId);
        List<VitalSignRecordDTO> out = repository.findTop10ByPatientIdOrderByTimestampDesc(patientId)
                .stream()
                .map(this::toSafeDTO)
                .toList();
        log.info("domain=vital_sign query=latest_by_patient success patientId={} count={}", patientId, out.size());
        return out;
    }

    public List<VitalSignRecordDTO> getHistoryByPatientId(String patientId) {
        log.info("domain=vital_sign query=history_by_patient start patientId={}", patientId);
        List<VitalSignRecordDTO> out = repository.findByPatientId(patientId)
                .stream()
                .map(this::toSafeDTO)
                .toList();
        log.info("domain=vital_sign query=history_by_patient success patientId={} count={}", patientId, out.size());
        return out;
    }

    public List<VitalSignRecordDTO> getLatestAll(int limit) {
        int safeLimit = Math.max(minLimit, Math.min(maxLimit, limit));
        log.info("domain=vital_sign query=latest_all start limit={} safeLimit={}", limit, safeLimit);
        List<VitalSignRecordDTO> out = repository
                .findAllByOrderByTimestampDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toSafeDTO)
                .toList();
        log.info("domain=vital_sign query=latest_all success safeLimit={} count={}", safeLimit, out.size());
        return out;
    }

    public List<VitalSignRecordDTO> getHistoryAll() {
        log.info("domain=vital_sign query=history_all start");
        List<VitalSignRecordDTO> out = repository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toSafeDTO)
                .toList();
        log.info("domain=vital_sign query=history_all success count={}", out.size());
        return out;
    }

    private String resolvePatientName(VitalSignRecordDTO in, Optional<VitalSignRecord> lastOpt) {
        if (!isBlank(in.patientName())) return in.patientName();
        log.debug("domain=vital_sign enrich=inherit_name patientId={}", in.patientId());
        return lastOpt.map(VitalSignRecord::getPatientName).orElse(null);
    }

    private String resolveEncryptedCpf(VitalSignRecordDTO in, Optional<VitalSignRecord> lastOpt) {
        String cpfIn = in.patientCpf();
        if (!isBlank(cpfIn)) {
            String digitsOnly = cpfIn.replaceAll("\\D", "");
            try {
                log.debug("domain=vital_sign encrypt=cpf source=payload patientId={}", in.patientId());
                return cryptoUtil.encrypt(digitsOnly);
            } catch (Exception e) {
                log.error("domain=vital_sign encrypt=fail patientId={} err='{}'", in.patientId(), e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao criptografar CPF");
            }
        }
        log.debug("domain=vital_sign enrich=inherit_cpf source=last_record patientId={}", in.patientId());
        return lastOpt.map(VitalSignRecord::getPatientCpf).orElse(null);
    }

    private LocalDateTime resolveTimestamp(String timestamp) {
        if (isBlank(timestamp)) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(timestamp);
        } catch (DateTimeParseException ex) {
            log.warn("domain=vital_sign validation=invalid_timestamp value='{}'", timestamp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timestamp em formato inválido (use ISO-8601)");
        }
    }

    private void validateFirstRecordRequireds(VitalSignRecord entity, boolean isFirstRecordForPatient) {
        if (!isFirstRecordForPatient) return;
        if (isBlank(entity.getPatientName())) {
            log.warn("domain=vital_sign validation=fail reason=missing_patientName patientId={}", entity.getPatientId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "patientName é obrigatório no primeiro registro do paciente " + entity.getPatientId()
            );
        }
        if (isBlank(entity.getPatientCpf())) {
            log.warn("domain=vital_sign validation=fail reason=missing_patientCpf patientId={}", entity.getPatientId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "patientCpf é obrigatório no primeiro registro do paciente " + entity.getPatientId()
            );
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
