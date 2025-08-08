package completo.projeto.completo.repository;

import completo.projeto.completo.entities.VitalSignRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VitalSignRecordRepository extends JpaRepository<VitalSignRecord, UUID> {

    // Por paciente
    List<VitalSignRecord> findTop10ByPatientIdOrderByTimestampDesc(String patientId);
    List<VitalSignRecord> findByPatientId(String patientId);

    // Globais (opcionais)
    Page<VitalSignRecord> findAllByOrderByTimestampDesc(Pageable pageable);
    List<VitalSignRecord> findAllByOrderByTimestampDesc();
}
