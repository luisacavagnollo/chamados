package br.com.sistemas.chamados.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;
import br.com.sistemas.chamados.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacSecurityTests {

    private static final String SENHA = "senha-de-teste-rbac";
    private static final String ADMIN_EMAIL = "admin.rbac@exemplo.invalid";
    private static final String CLIENTE_EMAIL = "cliente.rbac@exemplo.invalid";
    private static final String TECNICO_EMAIL = "tecnico.rbac@exemplo.invalid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    private Cliente cliente;
    private Categoria categoriaDoChamado;
    private Categoria categoriaParaAtualizar;
    private Categoria categoriaParaExcluir;
    private Tecnico tecnico;
    private Chamado chamadoAtribuido;
    private Chamado chamadoFechado;

    @BeforeEach
    void prepararIdentidadesEDados() {
        cliente = clienteRepository.save(
            new Cliente("Cliente RBAC", CLIENTE_EMAIL, null)
        );
        tecnico = tecnicoRepository.save(
            new Tecnico("Técnico RBAC", TECNICO_EMAIL, null)
        );
        categoriaDoChamado = categoriaRepository.save(
            new Categoria("Categoria usada no chamado RBAC", null)
        );
        categoriaParaAtualizar = categoriaRepository.save(
            new Categoria("Categoria a atualizar RBAC", null)
        );
        categoriaParaExcluir = categoriaRepository.save(
            new Categoria("Categoria a excluir RBAC", null)
        );

        usuarioService.criar(
            ADMIN_EMAIL,
            SENHA,
            PapelUsuario.ADMINISTRADOR,
            null,
            null
        );
        usuarioService.criar(
            CLIENTE_EMAIL,
            SENHA,
            PapelUsuario.CLIENTE,
            cliente,
            null
        );
        usuarioService.criar(
            TECNICO_EMAIL,
            SENHA,
            PapelUsuario.TECNICO,
            null,
            tecnico
        );

        chamadoAtribuido = chamadoRepository.save(new Chamado(
            "Chamado atribuído RBAC",
            "Chamado fictício de teste",
            Prioridade.MEDIA,
            cliente,
            categoriaDoChamado,
            tecnico
        ));
        chamadoFechado = new Chamado(
            "Chamado fechado RBAC",
            "Chamado fictício fechado para validar regra de negócio",
            Prioridade.BAIXA,
            cliente,
            categoriaDoChamado,
            tecnico
        );
        chamadoFechado.setStatus(StatusChamado.FECHADO);
        chamadoRepository.save(chamadoFechado);
    }

    @Test
    void administradorPodeConsultarECriarAtualizarExcluirCategorias() throws Exception {
        Cookie csrf = obterCookieCsrf(ADMIN_EMAIL);

        mockMvc.perform(get("/categorias").with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isOk());

        mockMvc.perform(escrita(post("/categorias"), ADMIN_EMAIL, csrf)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Categoria criada pelo administrador RBAC","descricao":"Teste"}
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(escrita(
                put("/categorias/{id}", categoriaParaAtualizar.getId()),
                ADMIN_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Categoria atualizada pelo administrador RBAC","descricao":"Atualizada"}
                """))
            .andExpect(status().isOk());

        mockMvc.perform(escrita(
                delete("/categorias/{id}", categoriaParaExcluir.getId()),
                ADMIN_EMAIL,
                csrf
            ))
            .andExpect(status().isNoContent());
    }

    @Test
    void clientePodeConsultarCategorias() throws Exception {
        mockMvc.perform(get("/categorias").with(httpBasic(CLIENTE_EMAIL, SENHA)))
            .andExpect(status().isOk());
    }

    @Test
    void tecnicoPodeConsultarCategorias() throws Exception {
        mockMvc.perform(get("/categorias").with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isOk());
    }

    @Test
    void clienteNaoPodeExecutarOperacoesAdministrativasMesmoComCsrfValido()
        throws Exception {
        Cookie csrf = obterCookieCsrf(CLIENTE_EMAIL);

        mockMvc.perform(escrita(post("/categorias"), CLIENTE_EMAIL, csrf)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nome":"Categoria não autorizada pelo cliente","descricao":"Teste"}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void administradorPodeConsultarECriarComentarios() throws Exception {
        Cookie csrf = obterCookieCsrf(ADMIN_EMAIL);

        mockMvc.perform(get(
                "/chamados/{chamadoId}/comentarios",
                chamadoAtribuido.getId()
            ).with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isOk());

        mockMvc.perform(escrita(
                post("/chamados/{chamadoId}/comentarios", chamadoAtribuido.getId()),
                ADMIN_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário administrativo de teste","autor":"Administrador RBAC"}
                """))
            .andExpect(status().isCreated());
    }

    @Test
    void tecnicoNaoPodeExecutarOperacoesAdministrativasMesmoComCsrfValido()
        throws Exception {
        Cookie csrf = obterCookieCsrf(TECNICO_EMAIL);

        mockMvc.perform(escrita(
                delete("/categorias/{id}", categoriaParaExcluir.getId()),
                TECNICO_EMAIL,
                csrf
            ))
            .andExpect(status().isForbidden());
    }

    @Test
    void endpointProtegidoRejeitaRequisicaoNaoAutenticada() throws Exception {
        mockMvc.perform(get("/categorias"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void acessosComEscopoDeClienteETecnicoSaoAutorizados()
        throws Exception {
        Cookie csrfCliente = obterCookieCsrf(CLIENTE_EMAIL);
        Cookie csrfTecnico = obterCookieCsrf(TECNICO_EMAIL);
        String chamadoId = chamadoAtribuido.getId().toString();

        mockMvc.perform(get("/chamados").with(httpBasic(CLIENTE_EMAIL, SENHA)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/clientes/{id}", cliente.getId())
                .with(httpBasic(CLIENTE_EMAIL, SENHA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(cliente.getId()));
        mockMvc.perform(get(
                "/chamados/{chamadoId}/comentarios",
                chamadoId
            ).with(httpBasic(CLIENTE_EMAIL, SENHA)))
            .andExpect(status().isOk());
        mockMvc.perform(get(
                "/chamados/{chamadoId}/comentarios",
                chamadoId
            ).with(httpBasic(TECNICO_EMAIL, SENHA)))
            .andExpect(status().isOk());

        mockMvc.perform(escrita(
                post("/chamados"),
                CLIENTE_EMAIL,
                csrfCliente
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "titulo":"Chamado não autorizado",
                  "descricao":"Teste",
                  "prioridade":"MEDIA",
                  "clienteId":%d,
                  "categoriaId":%d
                }
                """.formatted(cliente.getId(), categoriaDoChamado.getId())))
            .andExpect(status().isForbidden());

        mockMvc.perform(escrita(
                put("/chamados/{id}", chamadoId),
                CLIENTE_EMAIL,
                csrfCliente
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "titulo":"Atualização não autorizada",
                  "descricao":"Teste",
                  "prioridade":"MEDIA",
                  "clienteId":%d,
                  "categoriaId":%d
                }
                """.formatted(cliente.getId(), categoriaDoChamado.getId())))
            .andExpect(status().isForbidden());

        mockMvc.perform(escrita(
                patch("/chamados/{id}/status", chamadoId),
                TECNICO_EMAIL,
                csrfTecnico
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"EM_ATENDIMENTO"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));

        assertThat(chamadoRepository.findById(chamadoAtribuido.getId()))
            .get()
            .extracting(Chamado::getStatus)
            .isEqualTo(StatusChamado.EM_ATENDIMENTO);

        mockMvc.perform(escrita(
                post("/chamados/{chamadoId}/comentarios", chamadoId),
                CLIENTE_EMAIL,
                csrfCliente
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"texto":"Comentário não autorizado","autor":"Cliente RBAC"}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void administradorContinuaSujeitoAsRegrasDeNegocio() throws Exception {
        Cookie csrf = obterCookieCsrf(ADMIN_EMAIL);
        String body = """
            {
              "titulo":"Tentativa de alterar chamado fechado",
              "descricao":"Teste",
              "prioridade":"ALTA",
              "clienteId":%d,
              "categoriaId":%d
            }
            """.formatted(cliente.getId(), categoriaDoChamado.getId());

        mockMvc.perform(escrita(
                put("/chamados/{id}", chamadoFechado.getId()),
                ADMIN_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void rotaNaoMapeadaEnegadaMesmoParaAdministrador()
        throws Exception {
        mockMvc.perform(get("/recurso-nao-mapeado")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
    }

    @Test
    void rotasGetNovasDentroDasFamiliasExistentesSaoNegadas()
        throws Exception {
        mockMvc.perform(get("/categorias/resumo")
                .with(httpBasic(CLIENTE_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/categorias/resumo")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/chamados/relatorio")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/chamados/1/historico")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/clientes/resumo")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/tecnicos/disponiveis")
                .with(httpBasic(ADMIN_EMAIL, SENHA)))
            .andExpect(status().isForbidden());
    }

    @Test
    void rotasDeEscritaNovasDentroDasFamiliasExistentesSaoNegadas()
        throws Exception {
        Cookie csrf = obterCookieCsrf(ADMIN_EMAIL);
        mockMvc.perform(escrita(
                put("/categorias/{id}/ativar", categoriaParaAtualizar.getId()),
                ADMIN_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(escrita(
                post("/chamados/{id}/reabrir", chamadoAtribuido.getId()),
                ADMIN_EMAIL,
                csrf
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isForbidden());
    }

    private Cookie obterCookieCsrf(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(
            get("/categorias").with(httpBasic(email, SENHA))
        )
            .andExpect(status().isOk())
            .andReturn();
        Cookie cookie = resultado.getResponse().getCookie("XSRF-TOKEN");

        assertThat(cookie).as("cookie CSRF para %s", email).isNotNull();
        return cookie;
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
