package br.com.sistemas.chamados.exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class ErroRespostaFactory {

    private final ObjectMapper objectMapper;

    public ErroRespostaFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ErroResposta criar(
        HttpStatus status,
        String erro,
        List<String> detalhes
    ) {
        return new ErroResposta(Instant.now(), status.value(), erro, detalhes);
    }

    public void escrever(
        HttpServletResponse response,
        HttpStatus status,
        String erro
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
            response.getOutputStream(),
            criar(status, erro, List.of())
        );
    }
}
