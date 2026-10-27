package com.curso.domains.dtos;

public record TokenResponse(String token, String tipo, long expiresIn) {
}
