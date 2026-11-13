package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @NotBlank @Size(max = 100) String nome,
    @NotBlank @Email @Size(max = 120) String email,
    @Pattern(
        regexp = "(?:\\d{10,11}|\\(\\d{2}\\) \\d{4,5}-\\d{4})?",
        message = "O telefone deve incluir DDD e conter 10 ou 11 dígitos"
    )
    @Size(max = 20) String telefone
) { }