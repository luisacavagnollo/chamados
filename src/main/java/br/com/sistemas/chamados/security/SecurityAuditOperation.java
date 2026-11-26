package br.com.sistemas.chamados.security;

import java.util.Set;

public final class SecurityAuditOperation {

    private static final Set<String> METODOS_CONHECIDOS =
        Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private SecurityAuditOperation() { }

    public static String metodo(String method) {
        return METODOS_CONHECIDOS.contains(method) ? method : "OTHER";
    }

    public static String nome(String method, String path) {
        if ("GET".equals(method) && "/auth/me".equals(path)) {
            return "IDENTIDADE_ATUAL";
        }
        if ("GET".equals(method) && "/chamados".equals(path)) {
            return "CHAMADOS_LISTAR";
        }
        if ("GET".equals(method) && "/chamados/filtro".equals(path)) {
            return "CHAMADOS_FILTRAR";
        }
        if ("POST".equals(method) && "/chamados".equals(path)) {
            return "CHAMADO_CRIAR_ADMIN";
        }
        if ("POST".equals(method) && "/chamados/meus".equals(path)) {
            return "CHAMADO_CRIAR_PROPRIO";
        }
        if ("PUT".equals(method) && "/clientes/me".equals(path)) {
            return "CLIENTE_ATUALIZAR_PROPRIO";
        }
        if (path.matches("^/chamados/[0-9]+/comentarios$")) {
            return "CHAMADO_COMENTARIOS";
        }
        if (path.matches("^/chamados/[0-9]+/status$")
            && "PATCH".equals(method)) {
            return "CHAMADO_ALTERAR_STATUS";
        }
        if (path.matches("^/chamados/[0-9]+/tecnico$")
            && "PATCH".equals(method)) {
            return "CHAMADO_ATRIBUIR_TECNICO";
        }
        if (path.matches("^/chamados/meus/[0-9]+$")
            && "PUT".equals(method)) {
            return "CHAMADO_EDITAR_PROPRIO";
        }
        if (path.matches("^/chamados/[0-9]+$")) {
            return switch (method) {
                case "GET" -> "CHAMADO_CONSULTAR";
                case "PUT" -> "CHAMADO_EDITAR";
                case "DELETE" -> "CHAMADO_EXCLUIR";
                default -> "ROTA_DESCONHECIDA";
            };
        }
        if ("/categorias".equals(path)
            || path.matches("^/categorias/[0-9]+$")) {
            return "CATEGORIAS";
        }
        if ("/clientes".equals(path)
            || path.matches("^/clientes/[0-9]+$")) {
            return "CLIENTES";
        }
        if ("/tecnicos".equals(path)
            || path.matches("^/tecnicos/[0-9]+$")) {
            return "TECNICOS";
        }
        return "ROTA_DESCONHECIDA";
    }
}
