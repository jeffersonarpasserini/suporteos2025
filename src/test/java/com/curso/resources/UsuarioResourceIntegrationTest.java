package com.curso.resources;

import com.curso.domains.Usuario;
import com.curso.domains.enums.Papel;
import com.curso.repositories.UsuarioRepository;
import com.curso.suporteos2025.Suporteos2025Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Suporteos2025Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "administrador", roles = "ADMIN")
class UsuarioResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        usuarioExistente = usuarioRepository.save(new Usuario(
                null,
                "usuario.existente",
                passwordEncoder.encode("senha-atual"),
                Papel.USER,
                true
        ));
    }

    @Test
    @DisplayName("ADMIN deve criar usuario sem expor o hash da senha")
    void adminDeveCriarUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "novo.usuario",
                                  "password": "senha-segura",
                                  "papel": "USER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("novo.usuario"))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        Usuario salvo = usuarioRepository.findByUsername("novo.usuario").orElseThrow();
        assertThat(salvo.getSenha()).isNotEqualTo("senha-segura");
        assertThat(passwordEncoder.matches("senha-segura", salvo.getSenha())).isTrue();
    }

    @Test
    @DisplayName("ADMIN deve listar usuarios sem expor hashes")
    void adminDeveListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("usuario.existente"))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
    }

    @Test
    @DisplayName("ADMIN deve alterar papel e desativar usuario")
    void adminDeveAtualizarUsuario() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}", usuarioExistente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "usuario.atualizado",
                                  "papel": "ADMIN",
                                  "ativo": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario.atualizado"))
                .andExpect(jsonPath("$.papel").value("ADMIN"))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    @DisplayName("ADMIN deve alterar senha armazenando novo hash BCrypt")
    void adminDeveAlterarSenha() throws Exception {
        mockMvc.perform(put("/api/usuarios/{id}/senha", usuarioExistente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "nova-senha-segura" }
                                """))
                .andExpect(status().isNoContent());

        Usuario atualizado = usuarioRepository.findById(usuarioExistente.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nova-senha-segura", atualizado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "aluno", roles = "USER")
    @DisplayName("USER deve receber 403 ao tentar gerenciar usuarios")
    void userNaoDeveGerenciarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Usuario autenticado sem permissao para este recurso"));
    }
}
