package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.UsuarioMapper;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaService empresaService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarPorEmpresa(UUID empresaId, Pageable pageable) {
        return usuarioRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                .map(usuarioMapper::toResponse);
    }

    @Transactional
    public UsuarioResponse crearOperador(UUID empresaId, CreateUsuarioRequest request) {
        Empresa empresa = empresaService.findActiveOrThrow(empresaId);

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
                .empresa(empresa)
                .nombre(request.nombre())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(Rol.OPERADOR)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Operador creado: id={}, empresaId={}", usuario.getId(), empresaId);
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse getMe(Claims claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse updateMe(Claims claims, UpdatePerfilRequest request) {
        UUID userId = UUID.fromString(claims.getSubject());
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        usuario.setNombre(request.nombre());
        usuario.setTelefono(request.telefono());
        usuario = usuarioRepository.save(usuario);
        log.info("Perfil actualizado: userId={}", userId);
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarRol(UUID userId, Rol nuevoRol) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        usuario.setRol(nuevoRol);
        usuario = usuarioRepository.save(usuario);
        log.info("Rol cambiado: userId={}, nuevoRol={}", userId, nuevoRol);
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public void deshabilitar(UUID userId) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario deshabilitado (soft delete): id={}", userId);
    }

    @Transactional(readOnly = true)
    public Usuario findByIdOrThrow(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
