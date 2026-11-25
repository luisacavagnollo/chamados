package br.com.sistemas.chamados.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;

import br.com.sistemas.chamados.entity.PapelUsuario;
import br.com.sistemas.chamados.entity.Usuario;
import br.com.sistemas.chamados.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigTests {

    private static final String EMAIL_ATIVO = "ativo@teste.invalid";
    private static final String SENHA_ATIVA = "senha-de-teste";
    private static final String EMAIL_INATIVO = "inativo@teste.invalid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validator validator;

    @BeforeEach
    void criarIdentidades() {
        usuarioRepository.save(new Usuario(
            EMAIL_ATIVO,
            passwordEncoder.encode(SENHA_ATIVA),
            PapelUsuario.ADMINISTRADOR,
            true,
            null,
            null
        ));
        usuarioRepository.save(new Usuario(
            EMAIL_INATIVO,
            passwordEncoder.encode("outra-senha-de-teste"),
            PapelUsuario.ADMINISTRADOR,
            false,
            null,
            null
        ));
    }

    @Test
    void requisicaoSemAutenticacaoRecebeUnauthorized() throws Exception {
        mockMvc.perform(get("/chamados"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void credenciaisValidasPermitemLeitura() throws Exception {
        mockMvc.perform(get("/chamados").with(httpBasic(EMAIL_ATIVO, SENHA_ATIVA)))
            .andExpect(status().isOk());
    }

    @Test
    void credenciaisInvalidasRecebemUnauthorized() throws Exception {
        mockMvc.perform(get("/chamados").with(httpBasic(EMAIL_ATIVO, "senha-incorreta")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void contaDesativadaNaoConsegueAutenticar() throws Exception {
        mockMvc.perform(get("/chamados").with(
            httpBasic(EMAIL_INATIVO, "outra-senha-de-teste")
        )).andExpect(status().isUnauthorized());
    }

    @Test
    void escritaSemTokenCsrfRecebeForbidden() throws Exception {
        mockMvc.perform(post("/chamados")
            .with(httpBasic(EMAIL_ATIVO, SENHA_ATIVA))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void cookieCsrfPermiteEnviarTokenNoCabecalho() throws Exception {
        MvcResult respostaLeitura = mockMvc.perform(
            get("/chamados").with(httpBasic(EMAIL_ATIVO, SENHA_ATIVA))
        )
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();
        Cookie cookieCsrf = respostaLeitura.getResponse().getCookie("XSRF-TOKEN");

        assertThat(cookieCsrf).isNotNull();
        mockMvc.perform(post("/chamados")
            .with(httpBasic(EMAIL_ATIVO, SENHA_ATIVA))
            .cookie(cookieCsrf)
            .header("X-XSRF-TOKEN", cookieCsrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void identidadeDeClienteExigeVinculoComCliente() {
        Usuario usuario = new Usuario(
            "sem-vinculo@teste.invalid",
            passwordEncoder.encode("senha-de-teste"),
            PapelUsuario.CLIENTE,
            true,
            null,
            null
        );

        assertThat(validator.validate(usuario))
            .anyMatch(violacao ->
                violacao.getPropertyPath().toString().equals("vinculosConsistentes")
            );
    }
}
