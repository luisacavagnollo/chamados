package br.com.sistemas.chamados.exception;

import java.time.Instant;
import java.util.List;

public record ErroResposta(
    Instant timestamp,
    int status,
    String erro,
    List<String> detalhes
) { }
