package br.com.sistemas.chamados.config;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import br.com.sistemas.chamados.security.JsonAccessDeniedHandler;
import br.com.sistemas.chamados.security.JsonAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JsonAuthenticationEntryPoint authenticationEntryPoint,
        JsonAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler() {
                    @Override
                    public void handle(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Supplier<CsrfToken> deferredCsrfToken
                    ) {
                        super.handle(request, response, deferredCsrfToken);
                        deferredCsrfToken.get();
                    }
                })
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/auth/me")
                    .hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/categorias")
                    .hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    HttpMethod.GET,
                    "/chamados", "/chamados/filtro"
                ).hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    HttpMethod.GET,
                    "/clientes", "/tecnicos"
                ).hasRole("ADMINISTRADOR")
                .requestMatchers(
                    idEndpoint(HttpMethod.GET, "/categorias")
                ).hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    idEndpoint(HttpMethod.GET, "/chamados")
                ).hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    idEndpoint(HttpMethod.GET, "/clientes")
                ).hasAnyRole("CLIENTE", "ADMINISTRADOR")
                .requestMatchers(
                    idEndpoint(HttpMethod.GET, "/tecnicos"),
                    idEndpoint(HttpMethod.PUT, "/chamados"),
                    idEndpoint(HttpMethod.PUT, "/clientes"),
                    idEndpoint(HttpMethod.PUT, "/tecnicos"),
                    idEndpoint(HttpMethod.PUT, "/categorias"),
                    idEndpoint(HttpMethod.DELETE, "/chamados"),
                    idEndpoint(HttpMethod.DELETE, "/clientes"),
                    idEndpoint(HttpMethod.DELETE, "/tecnicos"),
                    idEndpoint(HttpMethod.DELETE, "/categorias")
                ).hasRole("ADMINISTRADOR")
                .requestMatchers(
                    HttpMethod.POST,
                    "/clientes",
                    "/tecnicos",
                    "/categorias"
                ).hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/chamados")
                    .hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/chamados/meus")
                    .hasRole("CLIENTE")
                .requestMatchers(
                    regexEndpoint(HttpMethod.PUT, "^/chamados/meus/[0-9]+$")
                ).hasRole("CLIENTE")
                .requestMatchers(HttpMethod.PUT, "/clientes/me")
                    .hasRole("CLIENTE")
                .requestMatchers(
                    regexEndpoint(HttpMethod.GET, "^/chamados/[0-9]+/comentarios$")
                ).hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    regexEndpoint(HttpMethod.POST, "^/chamados/[0-9]+/comentarios$")
                ).hasAnyRole("CLIENTE", "TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    regexEndpoint(
                        HttpMethod.PATCH,
                        "^/chamados/[0-9]+/status$"
                    )
                )
                    .hasAnyRole("TECNICO", "ADMINISTRADOR")
                .requestMatchers(
                    regexEndpoint(
                        HttpMethod.PATCH,
                        "^/chamados/[0-9]+/tecnico$"
                    )
                ).hasRole("ADMINISTRADOR")
                .anyRequest().denyAll()
            )
            .httpBasic(httpBasic -> httpBasic
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .build();
    }

    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher
    ) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }

    private static RegexRequestMatcher idEndpoint(HttpMethod method, String path) {
        return regexEndpoint(method, "^" + path + "/[0-9]+$");
    }

    private static RegexRequestMatcher regexEndpoint(HttpMethod method, String regex) {
        return RegexRequestMatcher.regexMatcher(method, regex);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
