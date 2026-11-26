package br.com.sistemas.chamados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.exception.AcessoNegadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.security.IdentidadeAutenticada;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final AutorizacaoRecursoService autorizacaoRecursoService;

    public ClienteService(
        ClienteRepository repository,
        AutorizacaoRecursoService autorizacaoRecursoService
    ) {
        this.repository = repository;
        this.autorizacaoRecursoService = autorizacaoRecursoService;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest dto) {
        autorizacaoRecursoService.exigirAdministrador(
            autorizacaoRecursoService.identidadeAtual()
        );
        if (repository.existsByEmail(dto.email())) {
            throw new RegraNegocioException("Já existe cliente com o e-mail " + dto.email());
        }
        Cliente salvo = repository.save(new Cliente(dto.nome(), dto.email(), dto.telefone()));
        return ClienteResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        autorizacaoRecursoService.exigirAdministrador(
            autorizacaoRecursoService.identidadeAtual()
        );
        return repository.findAll().stream().map(ClienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        IdentidadeAutenticada identidade =
            autorizacaoRecursoService.identidadeAtual();
        if (identidade.papel() != PapelUsuario.ADMINISTRADOR
            && identidade.papel() != PapelUsuario.CLIENTE) {
            throw new AcessoNegadoException();
        }

        Cliente cliente = buscarEntity(id);
        autorizacaoRecursoService.exigirAcessoAoCadastroCliente(
            identidade,
            cliente
        );
        return ClienteResponse.de(cliente);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest dto) {
        autorizacaoRecursoService.exigirAdministrador(
            autorizacaoRecursoService.identidadeAtual()
        );
        Cliente cliente = buscarEntity(id);
        if (repository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new RegraNegocioException("Já existe cliente com o e-mail " + dto.email());
        }
        cliente.setNome(dto.nome());
        cliente.setEmail(dto.email());
        cliente.setTelefone(dto.telefone());
        return ClienteResponse.de(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        autorizacaoRecursoService.exigirAdministrador(
            autorizacaoRecursoService.identidadeAtual()
        );
        repository.delete(buscarEntity(id));
    }

    private Cliente buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente " + id + " não encontrado"));
    }
}