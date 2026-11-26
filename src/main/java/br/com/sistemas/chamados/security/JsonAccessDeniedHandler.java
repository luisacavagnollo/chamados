package br.com.sistemas.chamados.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import br.com.sistemas.chamados.exception.ErroRespostaFactory;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private final ErroRespostaFactory erroRespostaFactory;

    public JsonAccessDeniedHandler(ErroRespostaFactory erroRespostaFactory) {
        this.erroRespostaFactory = erroRespostaFactory;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        String papel = authentication == null
            ? "ANONYMOUS"
            : authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .orElse("AUTHENTICATED");
        String evento = accessDeniedException instanceof CsrfException
            ? "CSRF_REJECTED"
            : "ACCESS_DENIED";
        String caminho = request.getRequestURI()
            .replaceAll("[^A-Za-z0-9/_{}.-]", "_");

        LOGGER.warn(
            "event={} method={} path={} role={}",
            evento,
            request.getMethod(),
            caminho,
            papel
        );
        erroRespostaFactory.escrever(
            response,
            HttpStatus.FORBIDDEN,
            "Acesso negado"
        );
    }
}
