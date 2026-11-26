
package br.com.sistemas.chamados.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
        @Value("${app.cors.allowed-origins:http://localhost:5173}")
        String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .toArray(String[]::new);
        if (this.allowedOrigins.length == 0
            || Arrays.stream(this.allowedOrigins)
                .anyMatch(origin -> origin.isBlank() || origin.equals("*"))) {
            throw new IllegalArgumentException(
                "Configure origens CORS explícitas em app.cors.allowed-origins"
            );
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
            .allowedHeaders(
                "Accept",
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN"
            )
            .exposedHeaders("WWW-Authenticate")
            .allowCredentials(true);
    }
}
