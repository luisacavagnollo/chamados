package br.com.sistemas.chamados.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.AcessoNegadoException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProtecaoPropriedadesTests {

    private static final String SENHA = "senha-ficticia-protecao";
    private static final String CLIENTE_EMAIL = "protecao.cliente@exemplo.invalid";
    private static final String OUTRO_CLIENTE_EMAIL = "protecao.outro@exemplo.invalid";
    private static final String TECNICO_EMAIL = "protecao.tecnico@exemplo.invalid";
    private static final String OUTRO_TECNICO_EMAIL = "protecao.outro.tecnico@exemplo.invalid";
    private static final String ADMIN_EMAIL = "protecao.admin@exemplo.invalid";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TecnicoRepository tecnicoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ChamadoRepository chamadoRepository;
    @Autowired private ComentarioRepository comentarioRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ChamadoService chamadoService;

    private Cliente cliente;
    private Cliente outroCliente;
    private Tecnico tecnico;
    private Tecnico outroTecnico;
    private Categoria categoria;
    private Chamado chamadoProprio;
    private Chamado chamadoAlheio;
    private Chamado chamadoFechado;

    @BeforeEach
    void prepararDados() {
        cliente = clienteRepository.save(
            new Cliente("Cliente de Teste", CLIENTE_EMAIL, "11911112222")
        );
        outroCliente = clienteRepository.save(
            new Cliente("Outro Cliente", OUTRO_CLIENTE_EMAIL, "11933334444")
        );
        tecnico = tecnicoRepository.save(
            new Tecnico("Técnica de Teste", TECNICO_EMAIL, "legado")
        );
        outroTecnico = tecnicoRepository.save(
            new Tecnico("Outro Técnico", OUTRO_TECNICO_EMAIL, null)
        );
        categoria = categoriaRepository.save(
            new Categoria("Categoria de Teste", "Dados fictícios")
        );

        usuarioService.criar(CLIENTE_EMAIL, SENHA, PapelUsuario.CLIENTE, cliente, null);
        usuarioService.criar(
            OUTRO_CLIENTE_EMAIL, SENHA, PapelUsuario.CLIENTE, outroCliente, null
        );
        usuarioService.criar(TECNICO_EMAIL, SENHA, PapelUsuario.TECNICO, null, tecnico);
        usuarioService.criar(
            OUTRO_TECNICO_EMAIL, SENHA, PapelUsuario.TECNICO, null, outroTecnico
        );
        usuarioService.criar(ADMIN_EMAIL, SENHA, PapelUsuario.ADMINISTRADOR, null, null);

        chamadoProprio = salvarChamado("Chamado próprio", cliente, tecnico, false);
        chamadoAlheio = salvarChamado("Chamado alheio", outroCliente, outroTecnico, false);
        chamadoFechado = salvarChamado("Chamado fechado", cliente, tecnico, true);
        comentarioRepository.save(new Comentario(
            "Comentário legado",
            "Autor legado preservado",
            chamadoProprio
        ));
    }

    @Test
    void clienteCriaChamadoProprioSemPermitirPropriedadesAdministrativas() throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        mockMvc.perform(escrita(post("/chamados/meus"), CLIENTE_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Novo chamado","descricao":"Problema fictício",
                     "prioridade":"ALTA","categoriaId":%d}
                    """.formatted(categoria.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
            .andExpect(jsonPath("$.tecnicoId").doesNotExist())
            .andExpect(jsonPath("$.status").value("ABERTO"));

        entityManager.flush();
        entityManager.clear();
        Chamado criado = chamadoRepository.findAll().stream()
            .filter(c -> c.getTitulo().equals("Novo chamado"))
            .findFirst()
            .orElseThrow();
        assertThat(criado.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(criado.getTecnico()).isNull();

        mockMvc.perform(escrita(post("/chamados/meus"), CLIENTE_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Tentativa","descricao":"Proprietário injetado",
                     "prioridade":"ALTA","categoriaId":%d,"clienteId":%d}
                    """.formatted(categoria.getId(), outroCliente.getId())))
            .andExpect(status().isBadRequest());
        mockMvc.perform(escrita(post("/chamados/meus"), CLIENTE_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Campos incompletos","prioridade":"ALTA","categoriaId":%d}
                    """.formatted(categoria.getId())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void clienteEditaChamadoProprioERejeitaCamposForaDoContrato() throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        mockMvc.perform(escrita(
                put("/chamados/meus/{id}", chamadoProprio.getId()), CLIENTE_EMAIL, csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"titulo":"Chamado editado","descricao":"Atualizado",
                 "prioridade":"ALTA","categoriaId":%d}
                """.formatted(categoria.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("Chamado editado"));

        entityManager.flush();
        entityManager.clear();
        Chamado persistido = chamadoRepository.findById(chamadoProprio.getId()).orElseThrow();
        assertThat(persistido.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(persistido.getTecnico().getId()).isEqualTo(tecnico.getId());
        assertThat(persistido.getStatus()).isEqualTo(StatusChamado.ABERTO);

        mockMvc.perform(escrita(
                put("/chamados/meus/{id}", chamadoProprio.getId()), CLIENTE_EMAIL, csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"titulo":"Tentativa","descricao":"Status injetado",
                 "prioridade":"ALTA","categoriaId":%d,"status":"FECHADO",
                 "tecnicoId":%d,"dataFechamento":"2026-01-01T10:00:00",
                 "id":999}
                """.formatted(categoria.getId(), outroTecnico.getId())))
            .andExpect(status().isBadRequest());

        entityManager.flush();
        entityManager.clear();
        assertThat(chamadoRepository.findById(chamadoProprio.getId()).orElseThrow().getTitulo())
            .isEqualTo("Chamado editado");
    }

    @Test
    void clienteNaoEditaChamadoAlheio() throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        mockMvc.perform(escrita(
                put("/chamados/meus/{id}", chamadoAlheio.getId()), CLIENTE_EMAIL, csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"titulo":"Não autorizado","descricao":"Teste",
                 "prioridade":"BAIXA","categoriaId":%d}
                """.formatted(categoria.getId())))
            .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();
        assertThat(chamadoRepository.findById(chamadoAlheio.getId()).orElseThrow().getTitulo())
            .isEqualTo("Chamado alheio");
    }

    @Test
    void endpointsAdministrativosExigemDadosObrigatorios() throws Exception {
        Cookie csrf = obterCookieCsrf(ADMIN_EMAIL);
        mockMvc.perform(escrita(post("/chamados"), ADMIN_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Sem proprietário","descricao":"Inválido",
                     "prioridade":"MEDIA","categoriaId":%d}
                    """.formatted(categoria.getId())))
            .andExpect(status().isBadRequest());
        mockMvc.perform(escrita(post("/clientes"), ADMIN_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Sem e-mail","telefone":"11911112222"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void clienteAtualizaCadastroProprioMasNaoPodeAlterarIdentidadeOuCredenciais()
        throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        mockMvc.perform(escrita(put("/clientes/me"), CLIENTE_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Nome atualizado","telefone":"11955556666"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Nome atualizado"))
            .andExpect(jsonPath("$.telefone").value("11955556666"));
        mockMvc.perform(escrita(put("/clientes/me"), CLIENTE_EMAIL, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"telefone":"11955556666"}
                    """))
            .andExpect(status().isBadRequest());

        for (String campoProtegido : List.of(
            "\"email\":\"novo@exemplo.invalid\"",
            "\"papel\":\"ADMINISTRADOR\"",
            "\"senha\":\"nova-senha\"",
            "\"senhaHash\":\"hash-injetado\"",
            "\"id\":999"
        )) {
            mockMvc.perform(escrita(put("/clientes/me"), CLIENTE_EMAIL, csrf)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nome":"Tentativa","telefone":"11955556666",%s}
                        """.formatted(campoProtegido)))
                .andExpect(status().isBadRequest());
        }

        entityManager.flush();
        entityManager.clear();
        Cliente persistido = clienteRepository.findById(cliente.getId()).orElseThrow();
        assertThat(persistido.getNome()).isEqualTo("Nome atualizado");
        assertThat(persistido.getEmail()).isEqualTo(CLIENTE_EMAIL);
    }

    @Test
    void edicaoDeChamadoFechadoContinuaNegada() throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        mockMvc.perform(escrita(
                put("/chamados/meus/{id}", chamadoFechado.getId()), CLIENTE_EMAIL, csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"titulo":"Não alterar","descricao":"Teste",
                 "prioridade":"BAIXA","categoriaId":%d}
                """.formatted(categoria.getId())))
            .andExpect(status().isConflict());
    }

    @Test
    void comentariosUsamNomeEIdentificadorInternoEProtegemResposta() throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        MvcResult resultado = mockMvc.perform(escrita(
                post("/chamados/{id}/comentarios", chamadoProprio.getId()),
                CLIENTE_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário autorizado"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.autor").value("Cliente de Teste"))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andExpect(content().string(not(containsString(CLIENTE_EMAIL))))
            .andReturn();

        entityManager.flush();
        entityManager.clear();
        Comentario comentario = comentarioRepository.findByChamadoIdOrderByDataAsc(
            chamadoProprio.getId()
        ).stream().filter(c -> c.getTexto().equals("Comentário autorizado"))
            .findFirst().orElseThrow();
        Long usuarioId = jdbcTemplate.queryForObject(
            "SELECT id FROM usuarios WHERE email = ?",
            Long.class,
            CLIENTE_EMAIL
        );
        assertThat(comentario.getAutorUsuario().getId()).isEqualTo(usuarioId);
        assertThat(comentario.getAutor()).isEqualTo("Cliente de Teste");
        assertThat(resultado.getResponse().getContentAsString())
            .doesNotContain(CLIENTE_EMAIL);
        Comentario legado = comentarioRepository.findByChamadoIdOrderByDataAsc(
            chamadoProprio.getId()
        ).stream().filter(c -> c.getTexto().equals("Comentário legado"))
            .findFirst().orElseThrow();
        assertThat(legado.getAutor()).isEqualTo("Autor legado preservado");
        assertThat(legado.getAutorUsuario()).isNull();

        Cookie csrfAdmin = obterCookieCsrf(ADMIN_EMAIL);
        mockMvc.perform(escrita(
                post("/chamados/{id}/comentarios", chamadoAlheio.getId()),
                ADMIN_EMAIL,
                csrfAdmin
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário administrativo"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.autor").value("Administrador"));

        mockMvc.perform(escrita(
                post("/chamados/{id}/comentarios", chamadoProprio.getId()),
                CLIENTE_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Spoofed","autor":"admin@exemplo.invalid"}
                """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void comentariosDeTecnicoAtribuidoSaoPermitidosEDeNaoAtribuidoNegados()
        throws Exception {
        Cookie csrfTecnico = obterCookieCsrf(TECNICO_EMAIL);
        mockMvc.perform(escrita(
                post("/chamados/{id}/comentarios", chamadoProprio.getId()),
                TECNICO_EMAIL,
                csrfTecnico
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário do técnico"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.autor").value("Técnica de Teste"));

        Cookie csrfOutroTecnico = obterCookieCsrf(OUTRO_TECNICO_EMAIL);
        mockMvc.perform(escrita(
                post("/chamados/{id}/comentarios", chamadoProprio.getId()),
                OUTRO_TECNICO_EMAIL,
                csrfOutroTecnico
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário não atribuído"}
                """))
            .andExpect(status().isForbidden());

        Cookie csrfOutroCliente = obterCookieCsrf(OUTRO_CLIENTE_EMAIL);
        mockMvc.perform(escrita(
                post("/chamados/{id}/comentarios", chamadoProprio.getId()),
                OUTRO_CLIENTE_EMAIL,
                csrfOutroCliente
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário de outro cliente"}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoPodeAlterarStatusNaApiNemDiretamenteNoServico() throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);
        mockMvc.perform(escrita(
                patch("/chamados/{id}/status", chamadoProprio.getId()),
                CLIENTE_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"FECHADO"}
                """))
            .andExpect(status().isForbidden());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                CLIENTE_EMAIL,
                SENHA,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
            )
        );
        try {
            assertThatThrownBy(() -> chamadoService.alterarStatus(
                chamadoProprio.getId(),
                new AlterarStatusRequest(StatusChamado.FECHADO)
            )).isInstanceOf(AcessoNegadoException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }

        entityManager.flush();
        entityManager.clear();
        Chamado persistido = chamadoRepository.findById(chamadoProprio.getId()).orElseThrow();
        assertThat(persistido.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(persistido.getDataFechamento()).isNull();
    }

    @Test
    void tecnicoAlteraStatusSomenteDeChamadoAtribuido() throws Exception {
        Cookie csrf = obterCookieCsrf(TECNICO_EMAIL);
        mockMvc.perform(escrita(
                patch("/chamados/{id}/status", chamadoProprio.getId()),
                TECNICO_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"EM_ATENDIMENTO"}
                """))
            .andExpect(status().isOk());

        mockMvc.perform(escrita(
                patch("/chamados/{id}/status", chamadoAlheio.getId()),
                TECNICO_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"FECHADO"}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void identidadeComVinculoInconsistenteNaoAutentica() throws Exception {
        jdbcTemplate.update(
            "UPDATE usuarios SET cliente_id = NULL WHERE email = ?",
            CLIENTE_EMAIL
        );
        entityManager.clear();
        mockMvc.perform(get("/categorias").with(httpBasic(CLIENTE_EMAIL, SENHA)))
            .andExpect(status().isUnauthorized());
    }

    private Chamado salvarChamado(
        String titulo,
        Cliente proprietario,
        Tecnico tecnicoAtribuido,
        boolean fechado
    ) {
        Chamado chamado = new Chamado(
            titulo,
            "Descrição fictícia",
            Prioridade.MEDIA,
            proprietario,
            categoria,
            tecnicoAtribuido
        );
        if (fechado) {
            chamado.setStatus(StatusChamado.FECHADO);
        }
        return chamadoRepository.save(chamado);
    }

    private Cookie obterCookieCsrf(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(
            get("/categorias").with(httpBasic(email, SENHA))
        ).andExpect(status().isOk()).andReturn();
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
