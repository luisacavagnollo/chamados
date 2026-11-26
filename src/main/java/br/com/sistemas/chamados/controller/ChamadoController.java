
package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoCriacaoClienteRequest;
import br.com.sistemas.chamados.dto.ChamadoEdicaoClienteRequest;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/chamados")
@Validated
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
        @RequestParam(required = false) @Min(0) Integer page,
        @RequestParam(required = false) @Min(1) @Max(100) Integer size,
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

    @PostMapping("/meus")
    public ResponseEntity<ChamadoResponse> criarProprio(
        @Valid @RequestBody ChamadoCriacaoClienteRequest dto
    ) {
        ChamadoResponse criado = service.criarComoCliente(dto);
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

    @PutMapping("/meus/{id}")
    public ChamadoResponse atualizarProprio(
        @PathVariable Long id,
        @Valid @RequestBody ChamadoEdicaoClienteRequest dto
    ) {
        return service.atualizarComoCliente(id, dto);
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
