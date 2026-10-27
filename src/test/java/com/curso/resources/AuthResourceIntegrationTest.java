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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Suporteos2025Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(
                null,
                "professor",
                passwordEncoder.encode("senha-segura"),
                Papel.ADMIN,
                true
        ));
        usuarioRepository.save(new Usuario(
                null,
                "aluno",
                passwordEncoder.encode("senha-do-aluno"),
                Papel.USER,
                true
        ));
    }

    @Test
    @DisplayName("POST /api/auth/login deve devolver um JWT para credenciais validas")
    void deveAutenticarEDevolverToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "professor",
                                  "password": "senha-segura"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/auth/login deve responder 401 para senha invalida")
    void deveRecusarSenhaInvalida() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "professor",
                                  "password": "senha-errada"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario ou senha invalidos"));
    }

    @Test
    @DisplayName("GET protegido deve responder 401 quando o token nao for enviado")
    void deveProtegerEndpointSemToken() throws Exception {
        mockMvc.perform(get("/api/grupoproduto/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Autenticacao necessaria ou token invalido"));
    }

    @Test
    @DisplayName("JWT emitido no login deve permitir acessar endpoint protegido")
    void deveAcessarEndpointProtegidoComToken() throws Exception {
        String token = realizarLogin();

        mockMvc.perform(get("/api/grupoproduto/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/auth/me deve identificar o usuario autenticado")
    void deveConsultarUsuarioAutenticado() throws Exception {
        String token = realizarLogin();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("professor"))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("DELETE protegido deve responder 403 para usuario sem papel ADMIN")
    void deveRecusarExclusaoSemPapelAdmin() throws Exception {
        String token = realizarLogin("aluno", "senha-do-aluno");

        mockMvc.perform(delete("/api/grupoproduto/{id}", 99999)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Usuario autenticado sem permissao para este recurso"));
    }

    @Test
    @DisplayName("POST protegido deve responder 403 para usuario sem papel ADMIN")
    void deveRecusarCriacaoSemPapelAdmin() throws Exception {
        String token = realizarLogin("aluno", "senha-do-aluno");

        mockMvc.perform(post("/api/grupoproduto")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "Grupo bloqueado",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuario desativado deve receber 401 ao tentar novo login")
    void deveRecusarLoginDeUsuarioDesativado() throws Exception {
        Usuario aluno = usuarioRepository.findByUsername("aluno").orElseThrow();
        aluno.setAtivo(false);
        usuarioRepository.saveAndFlush(aluno);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "aluno",
                                  "password": "senha-do-aluno"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String realizarLogin() throws Exception {
        return realizarLogin("professor", "senha-segura");
    }

    private String realizarLogin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.token"
        );
    }
}
