package br.com.sistemas.chamados.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.chamados.dto.AuthMeResponse;
import br.com.sistemas.chamados.service.UsuarioAutenticadoService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public AuthController(UsuarioAutenticadoService usuarioAutenticadoService) {
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @GetMapping("/me")
    public AuthMeResponse identidadeAtual() {
        return AuthMeResponse.de(
            usuarioAutenticadoService.obterIdentidadeAtual()
        );
    }
}
