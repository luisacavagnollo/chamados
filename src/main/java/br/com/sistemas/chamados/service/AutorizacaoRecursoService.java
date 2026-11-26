package br.com.sistemas.chamados.service;

import org.springframework.stereotype.Service;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.exception.AcessoNegadoException;
import br.com.sistemas.chamados.security.IdentidadeAutenticada;

@Service
public class AutorizacaoRecursoService {

    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public AutorizacaoRecursoService(
        UsuarioAutenticadoService usuarioAutenticadoService
    ) {
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    public IdentidadeAutenticada identidadeAtual() {
        return usuarioAutenticadoService.obterIdentidadeAtual();
    }

    public void exigirAdministrador(IdentidadeAutenticada identidade) {
        if (identidade.papel() != PapelUsuario.ADMINISTRADOR) {
            throw new AcessoNegadoException();
        }
    }

    public void exigirAcessoAoChamado(
        IdentidadeAutenticada identidade,
        Chamado chamado
    ) {
        boolean autorizado = switch (identidade.papel()) {
            case ADMINISTRADOR -> true;
            case CLIENTE -> identidade.clienteId() != null
                && identidade.clienteId().equals(chamado.getCliente().getId());
            case TECNICO -> identidade.tecnicoId() != null
                && chamado.getTecnico() != null
                && identidade.tecnicoId().equals(chamado.getTecnico().getId());
        };

        if (!autorizado) {
            throw new AcessoNegadoException();
        }
    }

    public void exigirAcessoAoCadastroCliente(
        IdentidadeAutenticada identidade,
        Cliente cliente
    ) {
        boolean autorizado = identidade.papel() == PapelUsuario.ADMINISTRADOR
            || (identidade.papel() == PapelUsuario.CLIENTE
                && identidade.clienteId() != null
                && identidade.clienteId().equals(cliente.getId()));

        if (!autorizado) {
            throw new AcessoNegadoException();
        }
    }

    public void exigirEdicaoChamadoCliente(
        IdentidadeAutenticada identidade,
        Chamado chamado
    ) {
        if (identidade.papel() != PapelUsuario.CLIENTE
            || identidade.clienteId() == null
            || !identidade.clienteId().equals(chamado.getCliente().getId())) {
            throw new AcessoNegadoException();
        }
    }

    public void exigirPodeComentar(
        IdentidadeAutenticada identidade,
        Chamado chamado
    ) {
        boolean autorizado = switch (identidade.papel()) {
            case ADMINISTRADOR -> true;
            case CLIENTE -> identidade.clienteId() != null
                && identidade.clienteId().equals(chamado.getCliente().getId());
            case TECNICO -> identidade.tecnicoId() != null
                && chamado.getTecnico() != null
                && identidade.tecnicoId().equals(chamado.getTecnico().getId());
        };
        if (!autorizado) {
            throw new AcessoNegadoException();
        }
    }
}
