package completo.projeto.completo.exception;

import java.time.LocalDateTime;

import javax.management.relation.RelationNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroPadrao> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErroPadrao erro = new ErroPadrao(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Requisição inválida", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroPadrao> handleGenericException(Exception ex) {
        ErroPadrao erro = new ErroPadrao(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

    @ExceptionHandler(RelationNotFoundException.class)
    public ResponseEntity<ErroPadrao> handleRelationNotFoundException(RelationNotFoundException ex) {
        ErroPadrao erro = new ErroPadrao(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Recurso não encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroPadrao> handleRequisicaoInvalida(RequisicaoInvalidaException ex) {
        ErroPadrao erro = new ErroPadrao(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Erro de validação", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroPadrao> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ErroPadrao erro = new ErroPadrao(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Recurso não encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErroPadrao> handleNoHandlerFound(NoHandlerFoundException ex) {
        ErroPadrao erro = new ErroPadrao(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Endpoint não encontrado",
                "A URL " + ex.getRequestURL() + " não existe."
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

}