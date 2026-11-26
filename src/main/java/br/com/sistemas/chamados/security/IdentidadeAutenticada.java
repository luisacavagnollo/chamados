package br.com.sistemas.chamados.security;

import br.com.sistemas.chamados.entity.PapelUsuario;

public record IdentidadeAutenticada(
    PapelUsuario papel,
    Long clienteId,
    Long tecnicoId
) { }
