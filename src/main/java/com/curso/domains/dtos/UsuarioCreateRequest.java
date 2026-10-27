package com.curso.domains.dtos;

import com.curso.domains.enums.Papel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank(message = "O username deve ser informado")
        @Size(max = 100, message = "O username deve possuir no maximo 100 caracteres")
        String username,

        @NotBlank(message = "A senha deve ser informada")
        @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres")
        String password,

        @NotNull(message = "O papel deve ser informado")
        Papel papel
) {
}
