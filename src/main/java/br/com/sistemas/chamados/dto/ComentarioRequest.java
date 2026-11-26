package br.com.sistemas.chamados.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(
    @NotBlank @Size(max = 1000) String texto
) {
    @JsonAnySetter
    public void rejeitarPropriedadeDesconhecida(String nome, Object valor) {
        throw new IllegalArgumentException("Propriedade não permitida: " + nome);
    }
}
