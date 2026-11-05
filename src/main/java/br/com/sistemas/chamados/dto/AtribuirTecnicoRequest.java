package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AtribuirTecnicoRequest(
    @NotNull @Positive Long tecnicoId
) { }