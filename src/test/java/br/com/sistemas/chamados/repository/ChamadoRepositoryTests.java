package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;

@SpringBootTest
@Transactional
class ChamadoRepositoryTests {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    private Cliente cliente;
    private Categoria categoria;
    private Tecnico tecnico;
    private Tecnico tecnicoSemChamados;

    @BeforeEach
    void prepararDados() {
        cliente = clienteRepository.save(
            new Cliente("Cliente", "cliente@teste.com", null)
        );
        categoria = categoriaRepository.save(
            new Categoria("Categoria", null)
        );
        Categoria outraCategoria = categoriaRepository.save(
            new Categoria("Outra categoria", null)
        );
        tecnico = tecnicoRepository.save(
            new Tecnico("Técnico", "tecnico@teste.com", null)
        );
        tecnicoSemChamados = tecnicoRepository.save(
            new Tecnico("Sem chamados", "sem-chamados@teste.com", null)
        );
        Tecnico outroTecnico = tecnicoRepository.save(
            new Tecnico("Outro técnico", "outro@teste.com", null)
        );

        chamadoRepository.save(new Chamado(
            "Correspondente 1", "Descrição", Prioridade.ALTA,
            cliente, categoria, tecnico
        ));
        chamadoRepository.save(new Chamado(
            "Correspondente 2", "Descrição", Prioridade.ALTA,
            cliente, categoria, tecnico
        ));
        chamadoRepository.save(new Chamado(
            "Outra categoria", "Descrição", Prioridade.ALTA,
            cliente, outraCategoria, tecnico
        ));
        chamadoRepository.save(new Chamado(
            "Outro técnico", "Descrição", Prioridade.ALTA,
            cliente, categoria, outroTecnico
        ));
        chamadoRepository.save(new Chamado(
            "Sem técnico", "Descrição", Prioridade.ALTA,
            cliente, categoria, null
        ));
    }

    @Test
    void filtraPorClienteTecnicoECategoriaComPaginacao() {
        Page<Chamado> primeiraPagina = chamadoRepository.filtrar(
            StatusChamado.ABERTO,
            Prioridade.ALTA,
            cliente.getId(),
            tecnico.getId(),
            categoria.getId(),
            PageRequest.of(
                0,
                1,
                Sort.by(Sort.Direction.DESC, "dataAbertura")
            )
        );

        assertThat(primeiraPagina.getContent()).hasSize(1);
        assertThat(primeiraPagina.getTotalElements()).isEqualTo(2);
        assertThat(primeiraPagina.getTotalPages()).isEqualTo(2);
        assertThat(primeiraPagina.getContent().get(0).getTecnico().getId())
            .isEqualTo(tecnico.getId());
        assertThat(primeiraPagina.getContent().get(0).getCategoria().getId())
            .isEqualTo(categoria.getId());

        Page<Chamado> semResultados = chamadoRepository.filtrar(
            null,
            null,
            null,
            tecnicoSemChamados.getId(),
            null,
            PageRequest.of(0, 10)
        );

        assertThat(semResultados.getContent()).isEmpty();
        assertThat(semResultados.getTotalElements()).isZero();

        Page<Chamado> semFiltros = chamadoRepository.filtrar(
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertThat(semFiltros.getTotalElements()).isEqualTo(5);
    }
}
