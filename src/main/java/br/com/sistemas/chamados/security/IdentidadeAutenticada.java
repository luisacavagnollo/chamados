package br.com.sistemas.chamados.security;

import br.com.sistemas.chamados.entity.PapelUsuario;

public record IdentidadeAutenticada(
    String email,
    PapelUsuario papel,
    Long clienteId,
    Long tecnicoId
) { }
