package br.com.sistemas.chamados.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;
import br.com.sistemas.chamados.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutorizacaoRecursoTests {

    private static final String SENHA = "senha-local-autorizacao";
    private static final String CLIENTE_1_EMAIL = "cliente1.recurso@exemplo.invalid";
    private static final String CLIENTE_2_EMAIL = "cliente2.recurso@exemplo.invalid";
    private static final String TECNICO_EMAIL = "tecnico.recurso@exemplo.invalid";
    private static final String ADMIN_EMAIL = "admin.recurso@exemplo.invalid";
    private static final String CLIENTE_INVALIDO_EMAIL =
        "cliente-invalido.recurso@exemplo.invalid";

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
    private ComentarioRepository comentarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioService usuarioService;

    private Cliente cliente1;
    private Cliente cliente2;
    private Tecnico tecnico1;
    private Categoria categoria;
    private Chamado chamadoCliente1Tecnico1;
    private Chamado chamadoCliente1OutroTecnico;
    private Chamado chamadoCliente2OutroTecnico;

    @BeforeEach
    void prepararDadosFicticios() {
        cliente1 = clienteRepository.save(
            new Cliente("Cliente Fictício Um", CLIENTE_1_EMAIL, "11900000001")
        );
        cliente2 = clienteRepository.save(
            new Cliente("Cliente Fictício Dois", CLIENTE_2_EMAIL, "11900000002")
        );
        tecnico1 = tecnicoRepository.save(
            new Tecnico("Técnico Fictício Um", TECNICO_EMAIL, "perfil-legado-ignorado")
        );
        Tecnico tecnico2 = tecnicoRepository.save(
            new Tecnico("Técnico Fictício Dois", "tecnico2.recurso@exemplo.invalid", null)
        );
        categoria = categoriaRepository.save(
            new Categoria("Categoria fictícia de autorização", "Teste local")
        );

        usuarioService.criar(
            CLIENTE_1_EMAIL,
            SENHA,
            PapelUsuario.CLIENTE,
            cliente1,
            null
        );
        usuarioService.criar(
            CLIENTE_2_EMAIL,
            SENHA,
            PapelUsuario.CLIENTE,
            cliente2,
            null
        );
        usuarioService.criar(
            TECNICO_EMAIL,
            SENHA,
            PapelUsuario.TECNICO,
            null,
            tecnico1
        );
        usuarioService.criar(
            ADMIN_EMAIL,
            SENHA,
            PapelUsuario.ADMINISTRADOR,
            null,
            null
        );

        chamadoCliente1Tecnico1 = salvarChamado(
            "Chamado reservado cliente um",
            cliente1,
            tecnico1,
            StatusChamado.ABERTO
        );
        chamadoCliente1OutroTecnico = salvarChamado(
            "Chamado cliente um atribuído a outro técnico",
            cliente1,
            tecnico2,
            StatusChamado.ABERTO
        );
        chamadoCliente2OutroTecnico = salvarChamado(
            "Chamado reservado cliente dois",
            cliente2,
            tecnico2,
            StatusChamado.ABERTO
        );

        comentarioRepository.save(new Comentario(
            "Comentário do chamado do cliente um",
            "Autor fictício",
            chamadoCliente1Tecnico1
        ));
        comentarioRepository.save(new Comentario(
            "Comentário do chamado do cliente dois",
            "Autor fictício",
            chamadoCliente2OutroTecnico
        ));
    }

    @Test
    void clienteConsultaChamadoProprioMasNaoChamadoDeOutroCliente() throws Exception {
        mockMvc.perform(get("/chamados/{id}", chamadoCliente1Tecnico1.getId())
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(chamadoCliente1Tecnico1.getId()))
            .andExpect(jsonPath("$.clienteId").value(cliente1.getId()))
            .andExpect(jsonPath("$.titulo").value("Chamado reservado cliente um"));

        mockMvc.perform(get("/chamados/{id}", chamadoCliente2OutroTecnico.getId())
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isForbidden())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Chamado reservado cliente dois")
            )));
    }

    @Test
    void clienteListaFiltraEPaginaSomenteSeusChamados() throws Exception {
        mockMvc.perform(get("/chamados").with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].clienteId").value(cliente1.getId()))
            .andExpect(jsonPath("$[1].clienteId").value(cliente1.getId()));

        mockMvc.perform(get("/chamados/filtro")
                .param("page", "0")
                .param("size", "1")
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].clienteId").value(cliente1.getId()))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/chamados/filtro")
                .param("clienteId", cliente2.getId().toString())
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void tecnicoConsultaChamadoAtribuidoMasNaoChamadoDeOutraAtribuicao()
        throws Exception {
        mockMvc.perform(get("/chamados/{id}", chamadoCliente1Tecnico1.getId())
                .with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(chamadoCliente1Tecnico1.getId()))
            .andExpect(jsonPath("$.tecnicoId").value(tecnico1.getId()));

        mockMvc.perform(get("/chamados/{id}", chamadoCliente1OutroTecnico.getId())
                .with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isForbidden())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString(
                    "Chamado cliente um atribuído a outro técnico"
                )
            )));
    }

    @Test
    void tecnicoListaSomenteChamadosAtribuidosAIdentidade() throws Exception {
        mockMvc.perform(get("/chamados").with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(chamadoCliente1Tecnico1.getId()));

        mockMvc.perform(get("/chamados/filtro")
                .param("tecnicoId", "999999")
                .with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void tecnicoAlteraStatusDeChamadoAtribuidoComCsrfValido() throws Exception {
        Cookie csrf = obterCookieCsrf(TECNICO_EMAIL);

        mockMvc.perform(escrita(
                patch("/chamados/{id}/status", chamadoCliente1Tecnico1.getId()),
                TECNICO_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"EM_ATENDIMENTO"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));

        assertThat(chamadoRepository.findById(chamadoCliente1Tecnico1.getId()))
            .get()
            .extracting(Chamado::getStatus)
            .isEqualTo(StatusChamado.EM_ATENDIMENTO);
    }

    @Test
    void tecnicoNaoAlteraChamadoNaoAtribuidoMesmoComCsrfValido() throws Exception {
        Cookie csrf = obterCookieCsrf(TECNICO_EMAIL);

        mockMvc.perform(escrita(
                patch("/chamados/{id}/status", chamadoCliente1OutroTecnico.getId()),
                TECNICO_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"EM_ATENDIMENTO"}
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.erro").value("Acesso negado"))
            .andExpect(jsonPath("$.detalhes", hasSize(0)));

        assertThat(chamadoRepository.findById(chamadoCliente1OutroTecnico.getId()))
            .get()
            .extracting(Chamado::getStatus)
            .isEqualTo(StatusChamado.ABERTO);
    }

    @Test
    void administradorConsultaChamadosDeClientesDiferentes() throws Exception {
        mockMvc.perform(get("/chamados").with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[?(@.clienteId == "
                + cliente1.getId() + ")]", hasSize(2)))
            .andExpect(jsonPath("$[?(@.clienteId == "
                + cliente2.getId() + ")]", hasSize(1)));
    }

    @Test
    void clienteConsultaSomenteSeuCadastro() throws Exception {
        mockMvc.perform(get("/clientes/{id}", cliente1.getId())
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(cliente1.getId()))
            .andExpect(jsonPath("$.email").value(CLIENTE_1_EMAIL));

        mockMvc.perform(get("/clientes/{id}", cliente2.getId())
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.erro").value("Acesso negado"))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString(CLIENTE_2_EMAIL)
            )));
    }

    @Test
    void leituraDeComentariosRespeitaAcessoAoChamado() throws Exception {
        mockMvc.perform(get(
                "/chamados/{id}/comentarios",
                chamadoCliente1Tecnico1.getId()
            ).with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].texto")
                .value("Comentário do chamado do cliente um"));

        mockMvc.perform(get(
                "/chamados/{id}/comentarios",
                chamadoCliente2OutroTecnico.getId()
            ).with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get(
                "/chamados/{id}/comentarios",
                chamadoCliente1Tecnico1.getId()
            ).with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(
                "/chamados/{id}/comentarios",
                chamadoCliente1OutroTecnico.getId()
            ).with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
    }

    @Test
    void inexistenteRetorna404ELeituraAnonimaRetorna401() throws Exception {
        mockMvc.perform(get("/chamados/999999")
                .with(httpBasic(CLIENTE_1_EMAIL, SENHA)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/chamados/{id}", chamadoCliente1Tecnico1.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void regrasDeNegocioContinuamAplicadasAoAdministrador() throws Exception {
        chamadoCliente1Tecnico1.setStatus(StatusChamado.FECHADO);
        chamadoRepository.saveAndFlush(chamadoCliente1Tecnico1);
        Cookie csrf = obterCookieCsrf(ADMIN_EMAIL);

        mockMvc.perform(escrita(
                put("/chamados/{id}", chamadoCliente1Tecnico1.getId()),
                ADMIN_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "titulo":"Alteração não permitida em fechado",
                  "descricao":"Teste",
                  "prioridade":"ALTA",
                  "clienteId":%d,
                  "categoriaId":%d
                }
                """.formatted(cliente1.getId(), categoria.getId())))
            .andExpect(status().isConflict());
    }

    @Test
    void identidadeComVinculoInconsistenteNaoRecebePermissao() throws Exception {
        Cliente clienteInvalido = clienteRepository.save(
            new Cliente(
                "Cliente com vínculo inconsistente",
                "cliente-invalido.recurso@exemplo.invalid",
                null
            )
        );
        usuarioService.criar(
            CLIENTE_INVALIDO_EMAIL,
            SENHA,
            PapelUsuario.CLIENTE,
            clienteInvalido,
            null
        );
        jdbcTemplate.update(
            "UPDATE usuarios SET cliente_id = NULL WHERE email = ?",
            CLIENTE_INVALIDO_EMAIL
        );
        entityManager.clear();

        mockMvc.perform(get("/chamados")
                .with(httpBasic(CLIENTE_INVALIDO_EMAIL, SENHA)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.erro").value("Acesso negado"));
    }

    private Chamado salvarChamado(
        String titulo,
        Cliente cliente,
        Tecnico tecnico,
        StatusChamado status
    ) {
        Chamado chamado = new Chamado(
            titulo,
            "Descrição fictícia para teste de autorização",
            Prioridade.MEDIA,
            cliente,
            categoria,
            tecnico
        );
        chamado.setStatus(status);
        return chamadoRepository.save(chamado);
    }

    private Cookie obterCookieCsrf(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(
            get("/categorias").with(httpBasic(email, SENHA))
        )
            .andExpect(status().isOk())
            .andReturn();
        Cookie csrf = resultado.getResponse().getCookie("XSRF-TOKEN");

        assertThat(csrf).isNotNull();
        return csrf;
    }

    private MockHttpServletRequestBuilder escrita(
        MockHttpServletRequestBuilder request,
        String email,
        Cookie csrf
    ) {
        return request
            .with(httpBasic(email, SENHA))
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue());
    }
}
