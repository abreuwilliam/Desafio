package completo.projeto.completo.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErroPadrao {
    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;
}
