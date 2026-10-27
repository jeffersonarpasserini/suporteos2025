package com.curso.domains.dtos;

import java.util.List;

public record UsuarioAutenticadoResponse(String username, List<String> authorities) {
}
