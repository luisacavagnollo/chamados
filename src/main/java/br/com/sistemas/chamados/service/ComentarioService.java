
package br.com.sistemas.chamados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.ComentarioRepository;

@Service
public class ComentarioService {

    private final ComentarioRepository repository;
    private final ChamadoService chamadoService;
    private final AutorizacaoRecursoService autorizacaoRecursoService;

    public ComentarioService(
        ComentarioRepository repository,
        ChamadoService chamadoService,
        AutorizacaoRecursoService autorizacaoRecursoService
    ) {
        this.repository = repository;
        this.chamadoService = chamadoService;
        this.autorizacaoRecursoService = autorizacaoRecursoService;
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> listarPorChamado(Long chamadoId) {
        chamadoService.buscarEntidadeAutorizada(chamadoId);

        return repository.findByChamadoIdOrderByDataAsc(chamadoId)
            .stream()
            .map(ComentarioResponse::de)
            .toList();
    }

    @Transactional
    public ComentarioResponse criar(
        Long chamadoId,
        ComentarioRequest dto
    ) {
        Chamado chamado = chamadoService.buscarEntidadeAutorizada(chamadoId);
        var identidade = autorizacaoRecursoService.identidadeAtual();
        autorizacaoRecursoService.exigirPodeComentar(identidade, chamado);

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException(
                "Não é possível comentar em um chamado fechado"
            );
        }

        Comentario comentario = new Comentario(
            dto.texto(),
            identidade.email(),
            chamado
        );

        return ComentarioResponse.de(repository.save(comentario));
    }

}
