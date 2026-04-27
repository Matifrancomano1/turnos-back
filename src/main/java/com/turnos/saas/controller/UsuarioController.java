package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.service.UsuarioService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/api/v1/empresas/{empresaId}/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
            @PathVariable UUID empresaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listarPorEmpresa(empresaId, pageable)));
    }

    @PostMapping("/api/v1/empresas/{empresaId}/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
            @PathVariable UUID empresaId,
            @Valid @RequestBody CreateUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Operador creado exitosamente",
                        usuarioService.crearOperador(empresaId, request)));
    }

    @GetMapping("/api/v1/usuarios/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> getMe(
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.getMe(claims)));
    }

    @PutMapping("/api/v1/usuarios/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> updateMe(
            @AuthenticationPrincipal Claims claims,
            @Valid @RequestBody UpdatePerfilRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.updateMe(claims, request)));
    }

    @PatchMapping("/api/v1/usuarios/{userId}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarRol(
            @PathVariable UUID userId,
            @RequestParam Rol rol) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.cambiarRol(userId, rol)));
    }

    @DeleteMapping("/api/v1/usuarios/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deshabilitar(@PathVariable UUID userId) {
        usuarioService.deshabilitar(userId);
        return ResponseEntity.noContent().build();
    }
}
