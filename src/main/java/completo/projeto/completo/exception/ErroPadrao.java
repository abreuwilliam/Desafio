package completo.projeto.completo.exception;

import java.time.LocalDateTime;

public class ErroPadrao {
    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;

    public ErroPadrao(LocalDateTime timestamp, int status, String erro, String mensagem) {
        this.timestamp = timestamp;
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
    }

    // Getters e setters

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
