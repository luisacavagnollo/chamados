package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
    @NotBlank @Size(max = 150) String titulo,
    @NotBlank @Size(max = 2000) String descricao,
    @NotNull Prioridade prioridade,
    @NotNull @Positive Long clienteId,
    @NotNull @Positive Long categoriaId,
    @Positive Long tecnicoId,
    /** Mantido só por compatibilidade com ChamadoService.atualizar; ignorado na criação. */
    StatusChamado status
) { }