
package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.service.ComentarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados/{chamadoId}/comentarios")
public class ComentarioController {

    private final ComentarioService service;

    public ComentarioController(ComentarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<ComentarioResponse> listar(
        @PathVariable Long chamadoId
    ) {
        return service.listarPorChamado(chamadoId);
    }

    @PostMapping
    public ResponseEntity<ComentarioResponse> criar(
        @PathVariable Long chamadoId,
        @Valid @RequestBody ComentarioRequest dto
    ) {
        ComentarioResponse criado = service.criar(chamadoId, dto);

        return ResponseEntity
            .created(URI.create(
                "/chamados/" + chamadoId +
                "/comentarios/" + criado.id()
            ))
            .body(criado);
    }
}
