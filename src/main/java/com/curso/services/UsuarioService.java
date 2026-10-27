package com.curso.services;

import com.curso.domains.Usuario;
import com.curso.domains.dtos.AlterarSenhaRequest;
import com.curso.domains.dtos.UsuarioCreateRequest;
import com.curso.domains.dtos.UsuarioResponse;
import com.curso.domains.dtos.UsuarioUpdateRequest;
import com.curso.repositories.UsuarioRepository;
import com.curso.services.exceptions.ObjectNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public UsuarioResponse create(UsuarioCreateRequest request) {
        validarUsernameDisponivel(request.username(), null);

        Usuario usuario = new Usuario(
                null,
                request.username(),
                passwordEncoder.encode(request.password()),
                request.papel(),
                true
        );
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = findEntity(id);
        validarUsernameDisponivel(request.username(), id);

        usuario.setUsername(request.username());
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequest request) {
        Usuario usuario = findEntity(id);
        usuario.setSenha(passwordEncoder.encode(request.password()));
        usuarioRepository.save(usuario);
    }

    private Usuario findEntity(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Usuario nao encontrado: id=" + id));
    }

    private void validarUsernameDisponivel(String username, Long idAtual) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            if (!usuario.getId().equals(idAtual)) {
                throw new DataIntegrityViolationException(
                        "Ja existe um usuario com o username informado"
                );
            }
        });
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPapel(),
                usuario.getAtivo()
        );
    }
}
