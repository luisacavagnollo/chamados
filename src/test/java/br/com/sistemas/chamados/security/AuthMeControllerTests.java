package br.com.sistemas.chamados.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AuthMeControllerTests {

    private static final String PASSWORD = "senha-auth-me-ficticia";
    private static final String CLIENTE_1_EMAIL = "auth.cliente1@exemplo.invalid";
    private static final String CLIENTE_2_EMAIL = "auth.cliente2@exemplo.invalid";
    private static final String TECNICO_EMAIL = "auth.tecnico@exemplo.invalid";
    private static final String ADMIN_EMAIL = "auth.admin@exemplo.invalid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private UsuarioService usuarioService;

    private Cliente cliente1;
    private Tecnico tecnico;
    private Chamado chamadoCliente2;

    @BeforeEach
    void prepararIdentidadesEDadosFicticios() {
        cliente1 = clienteRepository.save(
            new Cliente("Cliente Autenticado Fictício", CLIENTE_1_EMAIL, null)
        );
        Cliente cliente2 = clienteRepository.save(
            new Cliente("Outro Cliente Fictício", CLIENTE_2_EMAIL, null)
        );
        tecnico = tecnicoRepository.save(
            new Tecnico("Técnico Autenticado Fictício", TECNICO_EMAIL, null)
        );
        Categoria categoria = categoriaRepository.save(
            new Categoria("Categoria Auth fictícia", "Somente para testes")
        );

        usuarioService.criar(
            CLIENTE_1_EMAIL,
            PASSWORD,
            PapelUsuario.CLIENTE,
            cliente1,
            null
        );
        usuarioService.criar(
            CLIENTE_2_EMAIL,
            PASSWORD,
            PapelUsuario.CLIENTE,
            cliente2,
            null
        );
        usuarioService.criar(
            TECNICO_EMAIL,
            PASSWORD,
            PapelUsuario.TECNICO,
            null,
            tecnico
        );
        usuarioService.criar(
            ADMIN_EMAIL,
            PASSWORD,
            PapelUsuario.ADMINISTRADOR,
            null,
            null
        );

        chamadoCliente2 = chamadoRepository.save(new Chamado(
            "Chamado de outro cliente fictício",
            "Registro sem dados reais para testar negação por recurso",
            Prioridade.MEDIA,
            cliente2,
            categoria,
            tecnico
        ));
    }

    @Test
    void retornaSomenteOsCamposDaIdentidadeDosTresPapeis() throws Exception {
        MvcResult respostaCliente = mockMvc.perform(get("/auth/me")
                .with(httpBasic(CLIENTE_1_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andExpect(jsonPath("$.usuarioId").isNumber())
            .andExpect(jsonPath("$.nomeExibicao")
                .value("Cliente Autenticado Fictício"))
            .andExpect(jsonPath("$.papel").value("CLIENTE"))
            .andExpect(jsonPath("$.clienteId").value(cliente1.getId()))
            .andExpect(jsonPath("$.tecnicoId").doesNotExist())
            .andReturn();

        mockMvc.perform(get("/auth/me")
                .with(httpBasic(TECNICO_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nomeExibicao")
                .value("Técnico Autenticado Fictício"))
            .andExpect(jsonPath("$.papel").value("TECNICO"))
            .andExpect(jsonPath("$.clienteId").doesNotExist())
            .andExpect(jsonPath("$.tecnicoId").isNumber());

        MvcResult respostaAdministrador = mockMvc.perform(get("/auth/me")
                .with(httpBasic(ADMIN_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nomeExibicao").value("Administrador"))
            .andExpect(jsonPath("$.papel").value("ADMINISTRADOR"))
            .andExpect(jsonPath("$.clienteId").doesNotExist())
            .andExpect(jsonPath("$.tecnicoId").doesNotExist())
            .andReturn();

        for (String body : new String[] {
            respostaCliente.getResponse().getContentAsString(),
            respostaAdministrador.getResponse().getContentAsString()
        }) {
            assertThat(body)
                .doesNotContain("senha")
                .doesNotContain("email")
                .doesNotContain("senhaHash")
                .doesNotContain(CLIENTE_1_EMAIL)
                .doesNotContain(ADMIN_EMAIL);
        }
    }

    @Test
    void identidadeNaoPodeSerConsultadaSemAutenticacao() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.erro").value("Autenticação necessária"));
    }

    @Test
    void identidadeNaoPodeSerConsultadaComCredenciaisInvalidas() throws Exception {
        mockMvc.perform(get("/auth/me")
                .with(httpBasic(ADMIN_EMAIL, "senha-incorreta-ficticia")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.erro").value("Autenticação necessária"));
    }

    @Test
    void auditoriaDeRotaDesconhecidaNaoRegistraUrlBruta(
        CapturedOutput output
    ) throws Exception {
        String conteudoArbitrario = "conta-ficticia@exemplo.invalid";

        mockMvc.perform(get("/rota-desconhecida/" + conteudoArbitrario)
                .with(httpBasic(ADMIN_EMAIL, PASSWORD)))
            .andExpect(status().isForbidden());

        assertThat(output.getOut())
            .contains("event=ACCESS_DENIED")
            .contains("operation=ROTA_DESCONHECIDA")
            .doesNotContain(conteudoArbitrario)
            .doesNotContain(ADMIN_EMAIL);
    }

    @Test
    void negacaoPorRecursoNaCamadaServiceEhAuditadaSemDadosSensíveis(
        CapturedOutput output
    ) throws Exception {
        mockMvc.perform(get("/chamados/{id}", chamadoCliente2.getId())
                .with(httpBasic(CLIENTE_1_EMAIL, PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.erro").value("Acesso negado"));

        assertThat(output.getOut())
            .contains("event=SERVICE_ACCESS_DENIED")
            .contains("operation=CHAMADO_CONSULTAR")
            .contains("role=ROLE_CLIENTE")
            .doesNotContain(CLIENTE_1_EMAIL)
            .doesNotContain(CLIENTE_2_EMAIL)
            .doesNotContain(chamadoCliente2.getTitulo());
    }
}
