package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChamadoEdicaoClienteRequest(
    @NotBlank @Size(max = 150) String titulo,
    @NotBlank @Size(max = 2000) String descricao,
    @NotNull Prioridade prioridade,
    @NotNull @Positive Long categoriaId
) { }
