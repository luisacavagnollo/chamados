package br.com.sistemas.chamados.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteAtualizacaoRequest(
    @NotBlank @Size(max = 100) String nome,
    @Pattern(
        regexp = "(?:\\d{10,11}|\\(\\d{2}\\) \\d{4,5}-\\d{4})?",
        message = "O telefone deve incluir DDD e conter 10 ou 11 dígitos"
    )
    @Size(max = 20) String telefone
) {
    @JsonAnySetter
    public void rejeitarPropriedadeDesconhecida(String nome, Object valor) {
        throw new IllegalArgumentException("Propriedade não permitida: " + nome);
    }
}
