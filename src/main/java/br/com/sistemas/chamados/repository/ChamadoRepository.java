package br.com.sistemas.chamados.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    boolean existsByTituloAndClienteId(String titulo, Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "categoria", "tecnico"})
    Optional<Chamado> findByIdAndClienteId(Long id, Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "categoria", "tecnico"})
    Optional<Chamado> findByIdAndTecnicoId(Long id, Long tecnicoId);

    @EntityGraph(attributePaths = {"cliente", "categoria", "tecnico"})
    List<Chamado> findAllByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "categoria", "tecnico"})
    List<Chamado> findAllByTecnicoId(Long tecnicoId);

    @EntityGraph(attributePaths = {"cliente", "categoria", "tecnico"})
    @Query("""
        SELECT c FROM Chamado c
        LEFT JOIN c.tecnico tecnico
        LEFT JOIN c.categoria categoria
        WHERE (:status IS NULL OR c.status = :status)
          AND (:prioridade IS NULL OR c.prioridade = :prioridade)
          AND (:clienteId IS NULL OR c.cliente.id = :clienteId)
          AND (:tecnicoId IS NULL OR tecnico.id = :tecnicoId)
          AND (:categoriaId IS NULL OR categoria.id = :categoriaId)
          AND (:escopoClienteId IS NULL OR c.cliente.id = :escopoClienteId)
          AND (:escopoTecnicoId IS NULL OR tecnico.id = :escopoTecnicoId)
        """)
    Page<Chamado> filtrar(
        @Param("status") StatusChamado status,
        @Param("prioridade") Prioridade prioridade,
        @Param("clienteId") Long clienteId,
        @Param("tecnicoId") Long tecnicoId,
        @Param("categoriaId") Long categoriaId,
        @Param("escopoClienteId") Long escopoClienteId,
        @Param("escopoTecnicoId") Long escopoTecnicoId,
        Pageable pageable);
    @EntityGraph(attributePaths = {"cliente", "categoria", "tecnico"})
    Page<Chamado> findByClienteId(Long clienteId, Pageable pageable);
}