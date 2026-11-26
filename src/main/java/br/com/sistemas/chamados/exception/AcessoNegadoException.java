package br.com.sistemas.chamados.exception;

public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Acesso negado");
    }
}
