package br.com.sistemas.chamados.security;

import br.com.sistemas.chamados.entity.PapelUsuario;

public record IdentidadeAutenticada(
    Long usuarioId,
    String nomeExibicao,
    PapelUsuario papel,
    Long clienteId,
    Long tecnicoId
) { }
