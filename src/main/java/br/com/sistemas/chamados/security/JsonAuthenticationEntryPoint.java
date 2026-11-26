package br.com.sistemas.chamados.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import br.com.sistemas.chamados.exception.ErroRespostaFactory;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErroRespostaFactory erroRespostaFactory;

    public JsonAuthenticationEntryPoint(ErroRespostaFactory erroRespostaFactory) {
        this.erroRespostaFactory = erroRespostaFactory;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"chamados\"");
        erroRespostaFactory.escrever(
            response,
            HttpStatus.UNAUTHORIZED,
            "Autenticação necessária"
        );
    }
}
