package br.com.sistemas.chamados.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.sistemas.chamados.security.IdentidadeAutenticada;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthMeResponse(
    Long usuarioId,
    String nomeExibicao,
    String papel,
    Long clienteId,
    Long tecnicoId
) {
    public static AuthMeResponse de(IdentidadeAutenticada identidade) {
        return new AuthMeResponse(
            identidade.usuarioId(),
            identidade.nomeExibicao(),
            identidade.papel().name(),
            identidade.clienteId(),
            identidade.tecnicoId()
        );
    }
}
