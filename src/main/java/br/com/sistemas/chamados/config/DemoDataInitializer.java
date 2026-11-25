package br.com.sistemas.chamados.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;
import br.com.sistemas.chamados.service.UsuarioService;

@Configuration
@Profile("demo")
public class DemoDataInitializer {

    @Bean
    ApplicationRunner demoData(
        ClienteRepository clienteRepository,
        TecnicoRepository tecnicoRepository,
        CategoriaRepository categoriaRepository,
        ChamadoRepository chamadoRepository,
        UsuarioService usuarioService,
        @Value("${demo.password.cliente-1}") String senhaCliente1,
        @Value("${demo.password.cliente-2}") String senhaCliente2,
        @Value("${demo.password.tecnico}") String senhaTecnico,
        @Value("${demo.password.administrador}") String senhaAdministrador
    ) {
        return args -> {
            Cliente cliente1 = clienteRepository.findByEmail("ana.cliente@exemplo.invalid")
                .orElseGet(() -> clienteRepository.save(
                    new Cliente("Ana Demonstração", "ana.cliente@exemplo.invalid", null)
                ));
            Cliente cliente2 = clienteRepository.findByEmail("bruno.cliente@exemplo.invalid")
                .orElseGet(() -> clienteRepository.save(
                    new Cliente("Bruno Demonstração", "bruno.cliente@exemplo.invalid", null)
                ));
            Tecnico tecnico = tecnicoRepository.findByEmail("caio.tecnico@exemplo.invalid")
                .orElseGet(() -> tecnicoRepository.save(
                    new Tecnico("Caio Demonstração", "caio.tecnico@exemplo.invalid", null)
                ));

            criarUsuarioSeAusente(
                usuarioService,
                "ana.cliente@exemplo.invalid",
                senhaCliente1,
                PapelUsuario.CLIENTE,
                cliente1,
                null
            );
            criarUsuarioSeAusente(
                usuarioService,
                "bruno.cliente@exemplo.invalid",
                senhaCliente2,
                PapelUsuario.CLIENTE,
                cliente2,
                null
            );
            criarUsuarioSeAusente(
                usuarioService,
                "caio.tecnico@exemplo.invalid",
                senhaTecnico,
                PapelUsuario.TECNICO,
                null,
                tecnico
            );
            criarUsuarioSeAusente(
                usuarioService,
                "admin@exemplo.invalid",
                senhaAdministrador,
                PapelUsuario.ADMINISTRADOR,
                null,
                null
            );

            Categoria categoria = categoriaRepository.findByNome("Demonstração - Acesso")
                .orElseGet(() -> categoriaRepository.save(
                    new Categoria("Demonstração - Acesso", "Categoria fictícia para demonstração")
                ));

            criarChamadoSeAusente(
                chamadoRepository,
                "Demonstração - acesso ao sistema",
                cliente1,
                categoria,
                tecnico
            );
            criarChamadoSeAusente(
                chamadoRepository,
                "Demonstração - configuração de conta",
                cliente2,
                categoria,
                null
            );
        };
    }

    private void criarUsuarioSeAusente(
        UsuarioService usuarioService,
        String email,
        String senha,
        PapelUsuario papel,
        Cliente cliente,
        Tecnico tecnico
    ) {
        if (!usuarioService.existePorEmail(email)) {
            usuarioService.criar(email, senha, papel, cliente, tecnico);
        }
    }

    private void criarChamadoSeAusente(
        ChamadoRepository chamadoRepository,
        String titulo,
        Cliente cliente,
        Categoria categoria,
        Tecnico tecnico
    ) {
        if (!chamadoRepository.existsByTituloAndClienteId(titulo, cliente.getId())) {
            chamadoRepository.save(new Chamado(
                titulo,
                "Registro fictício criado pelo perfil local de demonstração.",
                Prioridade.MEDIA,
                cliente,
                categoria,
                tecnico
            ));
        }
    }
}
