package br.com.sistemas.chamados.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.validation.BindException;

import br.com.sistemas.chamados.security.SecurityAuditLogger;
import br.com.sistemas.chamados.security.SecurityAuditOperation;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErroRespostaFactory erroRespostaFactory;
    private final SecurityAuditLogger securityAuditLogger;

    public GlobalExceptionHandler(
        ErroRespostaFactory erroRespostaFactory,
        SecurityAuditLogger securityAuditLogger
    ) {
        this.erroRespostaFactory = erroRespostaFactory;
        this.securityAuditLogger = securityAuditLogger;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado() {
        return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado");
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(erroRespostaFactory.criar(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                List.of()
            ));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResposta> acessoNegado(
        HttpServletRequest request
    ) {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        String role = authentication == null
            ? "ANONYMOUS"
            : authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .orElse("AUTHENTICATED");

        securityAuditLogger.acessoNegadoNoServico(
            SecurityAuditOperation.metodo(request.getMethod()),
            SecurityAuditOperation.nome(
                request.getMethod(),
                request.getRequestURI()
            ),
            role
        );
        return montar(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    @ExceptionHandler({
        BindException.class,
        HandlerMethodValidationException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        TypeMismatchException.class
    })
    public ResponseEntity<ErroResposta> requisicaoInvalida(Exception ex) {
        List<String> detalhes = ex instanceof MethodArgumentNotValidException invalid
            ? invalid.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList()
            : List.of();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(erroRespostaFactory.criar(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                detalhes
            ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> metodoNaoSuportado() {
        return montar(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Método HTTP não permitido"
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> tipoMidiaNaoSuportado() {
        return montar(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Tipo de conteúdo não suportado"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> conflitoPersistencia() {
        return montar(
            HttpStatus.CONFLICT,
            "Conflito com os dados existentes"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> erroInterno(Exception ex) {
        LOGGER.error(
            "event=INTERNAL_ERROR exceptionType={}",
            ex.getClass().getSimpleName()
        );
        return montar(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno do servidor"
        );
    }

    private ResponseEntity<ErroResposta> montar(HttpStatus status, String erro) {
        return ResponseEntity.status(status)
            .body(erroRespostaFactory.criar(status, erro, List.of()));
    }
}