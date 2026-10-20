package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TecnicoRequest(
    @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,
    @NotBlank(message = "O email é obrigatório")
        @Email(message = "E-mail inválido")
        String email,
    @Size(max = 50, message = "O perfil deve ter no máximo 50 caracteres") String perfil
) { }

