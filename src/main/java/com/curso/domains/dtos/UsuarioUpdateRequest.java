package com.curso.domains.dtos;

import com.curso.domains.enums.Papel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank(message = "O username deve ser informado")
        @Size(max = 100, message = "O username deve possuir no maximo 100 caracteres")
        String username,

        @NotNull(message = "O papel deve ser informado")
        Papel papel,

        @NotNull(message = "A situacao do usuario deve ser informada")
        Boolean ativo
) {
}
