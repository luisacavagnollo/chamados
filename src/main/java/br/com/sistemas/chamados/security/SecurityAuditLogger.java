package br.com.sistemas.chamados.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.sistemas.chamados.repository.UsuarioRepository;

@Component
public class SecurityAuditLogger {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(SecurityAuditLogger.class);

    private final UsuarioRepository usuarioRepository;

    public SecurityAuditLogger(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @EventListener
    public void autenticacaoBemSucedida(AuthenticationSuccessEvent event) {
        usuarioRepository.findByEmail(event.getAuthentication().getName())
            .ifPresentOrElse(
                usuario -> LOGGER.info(
                    "event=AUTHENTICATION result=SUCCESS userId={} role={}",
                    usuario.getId(),
                    usuario.getPapel()
                ),
                () -> LOGGER.info(
                    "event=AUTHENTICATION result=SUCCESS userId=unavailable"
                )
            );
    }

    @EventListener
    public void autenticacaoRejeitada(AbstractAuthenticationFailureEvent event) {
        LOGGER.warn("event=AUTHENTICATION result=FAILURE");
    }

    public void acessoNegadoNoServico(
        String method,
        String operation,
        String role
    ) {
        LOGGER.warn(
            "event=SERVICE_ACCESS_DENIED method={} operation={} role={}",
            method,
            operation,
            role
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void chamadoAlterado(SecurityAuditEvent event) {
        LOGGER.info(
            "event=CHAMADO operation={} result=SUCCESS userId={} role={} chamadoId={}",
            event.operacao(),
            event.usuarioId(),
            event.papel(),
            event.chamadoId()
        );
    }
}
