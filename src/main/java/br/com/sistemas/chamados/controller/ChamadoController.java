
package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.ChamadoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChamadoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/filtro")
    public Page<ChamadoResponse> filtrar(
        @RequestParam(required = false) StatusChamado status,
        @RequestParam(required = false) Prioridade prioridade,
        @RequestParam(required = false) Long clienteId,
        @RequestParam(required = false) Long tecnicoId,
        @RequestParam(required = false) Long categoriaId,
        Pageable pageable
    ) {
        return service.filtrar(
            status,
            prioridade,
            clienteId,
            tecnicoId,
            categoriaId,
            pageable
        );
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(
        @Valid @RequestBody ChamadoRequest dto
    ) {
        ChamadoResponse criado = service.criar(dto);

        return ResponseEntity
            .created(URI.create("/chamados/" + criado.id()))
            .body(criado);
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(
        @PathVariable Long id,
        @Valid @RequestBody ChamadoRequest dto
    ) {
        return service.atualizar(id, dto);
    }

    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(
        @PathVariable Long id,
        @Valid @RequestBody AlterarStatusRequest dto
    ) {
        return service.alterarStatus(id, dto);
    }

    @PatchMapping("/{id}/tecnico")
    public ChamadoResponse atribuirTecnico(
        @PathVariable Long id,
        @Valid @RequestBody AtribuirTecnicoRequest dto
    ) {
        return service.atribuirTecnico(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
