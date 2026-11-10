
package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;

import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;

import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ComentarioRepository comentarioRepository;

    public ChamadoService(
        ChamadoRepository repository,
        ClienteRepository clienteRepository,
        CategoriaRepository categoriaRepository,
        TecnicoRepository tecnicoRepository,
        ComentarioRepository comentarioRepository
    ) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {

        Cliente cliente = buscarCliente(dto.clienteId());
        Categoria categoria = buscarCategoria(dto.categoriaId());
        Tecnico tecnico = buscarTecnico(dto.tecnicoId());

        Chamado chamado = new Chamado(
            dto.titulo(),
            dto.descricao(),
            dto.prioridade(),
            cliente,
            categoria,
            tecnico
        );

        Chamado salvo = repository.save(chamado);

        return ChamadoResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listar() {
        return repository.findAll()
            .stream()
            .map(ChamadoResponse::de)
            .toList();
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(buscarEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> filtrar(
        StatusChamado status,
        Prioridade prioridade,
        Long clienteId,
        Pageable pageable
    ) {
        return repository
            .filtrar(status, prioridade, clienteId, pageable)
            .map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listarPorCliente(
        Long clienteId,
        Pageable pageable
    ) {
        buscarCliente(clienteId);

        return repository
            .findByClienteId(clienteId, pageable)
            .map(ChamadoResponse::de);
    }

    @Transactional
    public ChamadoResponse atualizar(
        Long id,
        ChamadoRequest dto
    ) {
        Chamado chamado = buscarEntity(id);

        verificarSeAbertoParaAlteracoes(chamado);

        Cliente cliente = buscarCliente(dto.clienteId());
        Categoria categoria = buscarCategoria(dto.categoriaId());
        Tecnico tecnico = buscarTecnico(dto.tecnicoId());

        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(cliente);
        chamado.setCategoria(categoria);
        chamado.setTecnico(tecnico);

        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse atribuirTecnico(
        Long chamadoId,
        AtribuirTecnicoRequest dto
    ) {
        Chamado chamado = buscarEntity(chamadoId);

        verificarSeAbertoParaAlteracoes(chamado);

        Tecnico tecnico = buscarTecnico(dto.tecnicoId());

        chamado.setTecnico(tecnico);

        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(
        Long chamadoId,
        AlterarStatusRequest dto
    ) {
        Chamado chamado = buscarEntity(chamadoId);

        verificarSeAbertoParaAlteracoes(chamado);

        StatusChamado novoStatus = dto.status();

        if (novoStatus == null) {
            throw new RegraNegocioException(
                "O novo status é obrigatório"
            );
        }

        chamado.setStatus(novoStatus);

        if (novoStatus == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        } else {
            chamado.setDataFechamento(null);
        }

        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        Chamado chamado = buscarEntity(id);

        if (!comentarioRepository.findByChamadoId(id).isEmpty()) {
            throw new RegraNegocioException(
                "Não é possível excluir um chamado com comentários"
            );
        }

        repository.delete(chamado);
    }

    private void verificarSeAbertoParaAlteracoes(Chamado chamado) {

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException(
                "Não é possível alterar um chamado fechado"
            );
        }
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Chamado " + id + " não encontrado"
                )
            );
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Cliente " + id + " não encontrado"
                )
            );
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Categoria " + id + " não encontrada"
                )
            );
    }

    private Tecnico buscarTecnico(Long id) {

        if (id == null) {
            return null;
        }

        return tecnicoRepository.findById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Técnico " + id + " não encontrado"
                )
            );
    }
}
