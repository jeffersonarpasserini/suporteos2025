package com.curso.config;

import com.curso.domains.Usuario;
import com.curso.domains.enums.Papel;
import com.curso.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

@Configuration
@Profile("dev")
public class AdminInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username}") String username,
            @Value("${app.bootstrap.admin.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void criarAdministradorInicial() {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException(
                    "APP_ADMIN_USERNAME e APP_ADMIN_PASSWORD devem ser informados"
            );
        }

        if (usuarioRepository.findByUsername(username).isPresent()) {
            return;
        }

        Usuario admin = new Usuario(
                null,
                username,
                passwordEncoder.encode(password),
                Papel.ADMIN,
                true
        );
        usuarioRepository.save(admin);
    }
}
