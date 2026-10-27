package com.curso.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @NotBlank(message = "A nova senha deve ser informada")
        @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres")
        String password
) {
}
