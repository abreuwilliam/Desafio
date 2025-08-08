package completo.projeto.completo.websocket;

import completo.projeto.completo.dto.VitalSignRecordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class VitalSignWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendToDashboard(VitalSignRecordDTO dto) {
        messagingTemplate.convertAndSend("/topic/dashboard", dto);
    }

    public void sendToPatient(String patientId, VitalSignRecordDTO dto) {
        messagingTemplate.convertAndSend("/topic/patient/" + patientId, dto);
    }
}

