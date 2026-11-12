package br.com.sistemas.chamados.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ClienteRequestTests {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void criarValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void fecharValidador() {
        validatorFactory.close();
    }

    @Test
    void aceitaTelefoneBrasileiroFormatado() {
        ClienteRequest request = new ClienteRequest(
            "Cliente",
            "cliente@exemplo.com",
            "(11) 99999-9999"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejeitaTelefoneComFormatoInvalido() {
        ClienteRequest request = new ClienteRequest(
            "Cliente",
            "cliente@exemplo.com",
            "(11) 123-4567"
        );

        assertThat(validator.validate(request))
            .anyMatch(violacao ->
                violacao.getPropertyPath().toString().equals("telefone")
            );
    }

    @Test
    void aceitaEmailComCaractereUnicode() {
        ClienteRequest request = new ClienteRequest(
            "Cliente",
            "jfgddfsdghjklç@oi",
            null
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
