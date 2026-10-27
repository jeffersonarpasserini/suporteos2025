package com.curso.domains.dtos;

import com.curso.domains.enums.Papel;

public record UsuarioResponse(Long id, String username, Papel papel, Boolean ativo) {
}
