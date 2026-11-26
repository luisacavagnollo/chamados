package br.com.sistemas.chamados.security;

import br.com.sistemas.chamados.entity.PapelUsuario;

public record SecurityAuditEvent(
    String operacao,
    Long usuarioId,
    PapelUsuario papel,
    Long chamadoId
) { }
