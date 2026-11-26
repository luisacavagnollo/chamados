package br.com.sistemas.chamados.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Usuario;
import br.com.sistemas.chamados.exception.ErroResposta;
import br.com.sistemas.chamados.exception.GlobalExceptionHandler;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.UsuarioRepository;
import br.com.sistemas.chamados.service.ChamadoService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class TratamentoErrosSegurancaTests {

    private static final String ADMIN_EMAIL = "erros.admin@exemplo.invalid";
    private static final String SENHA = "senha-de-teste-tratamento-erros";
    private static final String ORIGEM_PERMITIDA = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @MockitoBean
    private ChamadoService chamadoService;

    @BeforeEach
    void criarAdministrador() {
        usuarioRepository.save(new Usuario(
            ADMIN_EMAIL,
            passwordEncoder.encode(SENHA),
            PapelUsuario.ADMINISTRADOR,
            true,
            null,
            null
        ));
    }

    @Test
    void resposta401TemContratoGenericoEDesafioBasic() throws Exception {
        mockMvc.perform(get("/chamados"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().string("WWW-Authenticate", "Basic realm=\"chamados\""))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.erro").value("Autenticação necessária"))
            .andExpect(jsonPath("$.detalhes").isEmpty());
    }

    @Test
    void credenciaisInvalidasNaoRevelamIdentidadeNemSenhaNosLogs(
        CapturedOutput output
    ) throws Exception {
        mockMvc.perform(get("/categorias")
                .with(httpBasic(ADMIN_EMAIL, "senha-incorreta-ficticia")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.erro").value("Autenticação necessária"));

        assertThat(output.getOut())
            .contains("event=AUTHENTICATION result=FAILURE")
            .doesNotContain(ADMIN_EMAIL)
            .doesNotContain("senha-incorreta-ficticia");
    }

    @Test
    void respostas403DeAutorizacaoEDeCsrfSaoJsonGenerico(
        CapturedOutput output
    ) throws Exception {
        Cookie csrf = obterCookieCsrf();

        mockMvc.perform(escrita(post("/chamados/meus"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.erro").value("Acesso negado"))
            .andExpect(jsonPath("$.detalhes").isEmpty());

        mockMvc.perform(post("/chamados")
                .with(httpBasic(ADMIN_EMAIL, SENHA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.erro").value("Acesso negado"));

        assertThat(output.getOut())
            .contains("event=ACCESS_DENIED")
            .contains("event=CSRF_REJECTED")
            .doesNotContain(ADMIN_EMAIL)
            .doesNotContain(csrf.getValue());
    }

    @Test
    void errosDeValidacaoEJsonMalformadoRetornam400() throws Exception {
        Cookie csrf = obterCookieCsrf();

        mockMvc.perform(escrita(post("/clientes"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.erro").value("Dados inválidos"));

        mockMvc.perform(escrita(post("/clientes"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"","email":"invalido","telefone":"11900000000"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes").isNotEmpty());
    }

    @Test
    void errosDeDominioNaoExibemDadosDoRecurso() throws Exception {
        Cookie csrf = obterCookieCsrf();

        String body = mockMvc.perform(get("/categorias/999999")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.erro").value("Recurso não encontrado"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(body).doesNotContain("999999");

        doThrow(new RegraNegocioException("Conflito fictício"))
            .when(chamadoService)
            .criar(any(ChamadoRequest.class));
        mockMvc.perform(escrita(post("/chamados"), csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo":"Chamado fictício",
                      "descricao":"Dados artificiais para teste",
                      "prioridade":"MEDIA",
                      "clienteId":1,
                      "categoriaId":1
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.erro").value("Conflito fictício"));
    }

    @Test
    void tipoDeConteudoNaoSuportadoRetorna415() throws Exception {
        Cookie csrf = obterCookieCsrf();

        mockMvc.perform(escrita(post("/clientes"), csrf)
                .contentType(MediaType.TEXT_PLAIN)
                .content("{}"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415))
            .andExpect(jsonPath("$.erro").value("Tipo de conteúdo não suportado"));
    }

    @Test
    void falhaInternaRetorna500SemDetalhesTecnicos(
        CapturedOutput output
    ) throws Exception {
        doThrow(new IllegalStateException("segredo-ficticio-da-excecao"))
            .when(chamadoService)
            .listar();

        String body = mockMvc.perform(get("/chamados")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.erro").value("Erro interno do servidor"))
            .andExpect(jsonPath("$.detalhes").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(body)
            .doesNotContain("segredo-ficticio-da-excecao")
            .doesNotContain(ADMIN_EMAIL);
        assertThat(output.getOut())
            .contains("event=INTERNAL_ERROR exceptionType=IllegalStateException")
            .doesNotContain("segredo-ficticio-da-excecao")
            .doesNotContain(ADMIN_EMAIL);
    }

    @Test
    void metodoNaoSuportadoUsaOContratoPadronizado() {
        var resposta = globalExceptionHandler.metodoNaoSuportado();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ErroResposta erro = resposta.getBody();
        assertThat(erro).isNotNull();
        assertThat(erro.timestamp()).isNotNull();
        assertThat(erro.status()).isEqualTo(405);
        assertThat(erro.erro()).isEqualTo("Método HTTP não permitido");
        assertThat(erro.detalhes()).isEmpty();
    }

    @Test
    void conflitoDePersistenciaUsaRespostaGenerica() {
        var resposta = globalExceptionHandler.conflitoPersistencia();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().erro())
            .isEqualTo("Conflito com os dados existentes");
        assertThat(resposta.getBody().detalhes()).isEmpty();
    }

    @Test
    void corsAceitaSomenteOrigemEMetodosConfigurados() throws Exception {
        mockMvc.perform(options("/chamados")
                .header("Origin", ORIGEM_PERMITIDA)
                .header("Access-Control-Request-Method", "GET")
                .header(
                    "Access-Control-Request-Headers",
                    "authorization,content-type,x-xsrf-token"
                ))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Access-Control-Allow-Origin",
                ORIGEM_PERMITIDA
            ))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/chamados")
                .header("Origin", "https://origem-nao-permitida.invalid")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void paginaInvalidaRetorna400ETamanhoValidoPermaneceAbaixoDoLimite()
        throws Exception {
        mockMvc.perform(get("/chamados/filtro")
                .param("page", "-1")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/chamados/filtro")
                    .param("page", "nao-numero")
                    .with(httpBasic(ADMIN_EMAIL, SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/chamados/filtro")
                .param("size", "101")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        doAnswer(invocation ->
            Page.empty((Pageable) invocation.getArgument(5))
        ).when(chamadoService).filtrar(
            nullable(StatusChamado.class),
            nullable(Prioridade.class),
            nullable(Long.class),
            nullable(Long.class),
            nullable(Long.class),
            any(Pageable.class)
        );
        mockMvc.perform(get("/chamados/filtro")
                .param("page", "2")
                .param("size", "100")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isOk());

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(chamadoService).filtrar(
            nullable(StatusChamado.class),
            nullable(Prioridade.class),
            nullable(Long.class),
            nullable(Long.class),
            nullable(Long.class),
            pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void logDeAlteracaoDeChamadoSoEEmitidoAposCommitSemDadosPessoais(
        CapturedOutput output
    ) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        transaction.executeWithoutResult(status -> eventPublisher.publishEvent(
            new SecurityAuditEvent(
                "EDICAO",
                42L,
                PapelUsuario.ADMINISTRADOR,
                73L
            )
        ));

        assertThat(output.getOut())
            .contains(
                "event=CHAMADO operation=EDICAO result=SUCCESS userId=42 "
                    + "role=ADMINISTRADOR chamadoId=73"
            )
            .doesNotContain(ADMIN_EMAIL)
            .doesNotContain(SENHA);
    }

    @Test
    void profileDeTesteUsaBancoIsoladoSemConsoleOuSqlNoLog() {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("spring.datasource.url"))
            .startsWith("jdbc:h2:mem:chamados-test");
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class))
            .isFalse();
        assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class))
            .isFalse();
        assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class))
            .isFalse();
    }

    @Test
    void autenticacaoBemSucedidaRegistraSomenteIdEPapel(
        CapturedOutput output
    ) throws Exception {
        mockMvc.perform(get("/categorias")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isOk());

        assertThat(output.getOut())
            .contains("event=AUTHENTICATION result=SUCCESS userId=")
            .contains("role=ADMINISTRADOR")
            .doesNotContain(ADMIN_EMAIL)
            .doesNotContain(SENHA);
    }

    private Cookie obterCookieCsrf() throws Exception {
        MvcResult resultado = mockMvc.perform(
            get("/categorias").with(httpBasic(ADMIN_EMAIL, SENHA))
        )
            .andExpect(status().isOk())
            .andReturn();
        Cookie cookie = resultado.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private MockHttpServletRequestBuilder escrita(
        MockHttpServletRequestBuilder request,
        Cookie csrf
    ) {
        return request
            .with(httpBasic(ADMIN_EMAIL, SENHA))
            .cookie(csrf)
            .header("X-XSRF-TOKEN", csrf.getValue());
    }
}
