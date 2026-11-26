package br.com.sistemas.chamados.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Usuario;
import br.com.sistemas.chamados.exception.AcessoNegadoException;
import br.com.sistemas.chamados.repository.UsuarioRepository;
import br.com.sistemas.chamados.security.IdentidadeAutenticada;

@Service
public class UsuarioAutenticadoService {

    private final UsuarioRepository repository;

    public UsuarioAutenticadoService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public IdentidadeAutenticada obterIdentidadeAtual() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken
            || authentication.getName() == null
            || authentication.getName().isBlank()) {
            throw new AcessoNegadoException();
        }

        Usuario usuario = repository.findByEmail(authentication.getName())
            .orElseThrow(AcessoNegadoException::new);
        String autoridadeEsperada = "ROLE_" + usuario.getPapel();

        if (!usuario.isAtivo()
            || !usuario.isVinculosConsistentes()
            || !authentication.getAuthorities().contains(
                new SimpleGrantedAuthority(autoridadeEsperada)
            )) {
            throw new AcessoNegadoException();
        }

        return new IdentidadeAutenticada(
            usuario.getId(),
            nomeExibicao(usuario),
            usuario.getPapel(),
            usuario.getCliente() == null ? null : usuario.getCliente().getId(),
            usuario.getTecnico() == null ? null : usuario.getTecnico().getId()
        );
    }

    @Transactional(readOnly = true)
    public Usuario obterUsuarioAtual() {
        Long usuarioId = obterIdentidadeAtual().usuarioId();
        return repository.findById(usuarioId)
            .orElseThrow(AcessoNegadoException::new);
    }

    private String nomeExibicao(Usuario usuario) {
        return switch (usuario.getPapel()) {
            case CLIENTE -> usuario.getCliente().getNome();
            case TECNICO -> usuario.getTecnico().getNome();
            case ADMINISTRADOR -> "Administrador";
        };
    }
}
