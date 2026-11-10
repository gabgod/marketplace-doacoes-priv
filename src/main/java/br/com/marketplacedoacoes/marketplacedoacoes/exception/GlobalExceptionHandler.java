package br.com.marketplacedoacoes.marketplacedoacoes.exception;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.ErroResposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> tratarConflito(ConflitoException ex) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue();
        return montar(HttpStatus.BAD_REQUEST, mensagem, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados", ex);
        return montar(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(AccessDeniedException ex) {
        return montar(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> tratarAutenticacao(AuthenticationException ex) {
        return montar(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarGenerico(Exception ex) {
        if (ex instanceof ErrorResponse erroSpring) {
            HttpStatus status = HttpStatus.valueOf(erroSpring.getStatusCode().value());
            return montar(status, erroSpring.getBody().getDetail(), null);
        }

        log.error("Erro inesperado", ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", null);
    }

    private ResponseEntity<ErroResposta> montar(HttpStatus status, String mensagem, Map<String, String> campos) {
        ErroResposta corpo = new ErroResposta(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                campos
        );
        return ResponseEntity.status(status).body(corpo);
    }
}