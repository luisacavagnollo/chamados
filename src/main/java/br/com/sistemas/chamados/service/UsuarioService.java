package br.com.sistemas.chamados.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.entity.Usuario;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
        UsuarioRepository repository,
        PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Transactional
    public Usuario criar(
        String email,
        String senha,
        PapelUsuario papel,
        Cliente cliente,
        Tecnico tecnico
    ) {
        if (repository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe uma identidade com esse e-mail");
        }
        if (papel == null) {
            throw new RegraNegocioException("O papel da identidade é obrigatório");
        }

        boolean vinculosValidos = switch (papel) {
            case CLIENTE -> cliente != null && tecnico == null;
            case TECNICO -> tecnico != null && cliente == null;
            case ADMINISTRADOR -> cliente == null && tecnico == null;
        };
        if (!vinculosValidos) {
            throw new RegraNegocioException("Os vínculos da identidade não correspondem ao papel");
        }

        return repository.save(new Usuario(
            email,
            passwordEncoder.encode(senha),
            papel,
            true,
            cliente,
            tecnico
        ));
    }
}
