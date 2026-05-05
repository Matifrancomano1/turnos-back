package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.security.TenantGuard;
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
    private final TenantGuard tenantGuard;

    // ── GET /api/v1/empresas/{empresaId}/usuarios ─────────────────────────────
    @GetMapping("/api/v1/empresas/{empresaId}/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal Claims claims,
            @PageableDefault(size = 20) Pageable pageable) {
        tenantGuard.assertAccess(empresaId, claims);
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listarPorEmpresa(empresaId, pageable)));
    }

    // ── POST /api/v1/empresas/{empresaId}/usuarios ────────────────────────────
    @PostMapping("/api/v1/empresas/{empresaId}/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal Claims claims,
            @Valid @RequestBody CreateUsuarioRequest request) {
        tenantGuard.assertAccess(empresaId, claims);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Usuario creado exitosamente",
                        usuarioService.crearUsuario(empresaId, request)));
    }

    // ── PATCH /api/v1/empresas/{empresaId}/usuarios/{userId}/activo ───────────
    @PatchMapping("/api/v1/empresas/{empresaId}/usuarios/{userId}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> toggleActivo(
            @PathVariable UUID empresaId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Claims claims,
            @RequestParam boolean activo) {
        tenantGuard.assertAccess(empresaId, claims);
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.toggleActivo(userId, activo)));
    }

    // ── GET /api/v1/usuarios/me ───────────────────────────────────────────────
    @GetMapping("/api/v1/usuarios/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> getMe(
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.getMe(claims)));
    }

    // ── PUT /api/v1/usuarios/me ───────────────────────────────────────────────
    @PutMapping("/api/v1/usuarios/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> updateMe(
            @AuthenticationPrincipal Claims claims,
            @Valid @RequestBody UpdatePerfilRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.updateMe(claims, request)));
    }

    // ── PATCH /api/v1/usuarios/{userId}/rol ──────────────────────────────────
    @PatchMapping("/api/v1/usuarios/{userId}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarRol(
            @PathVariable UUID userId,
            @RequestParam Rol rol) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.cambiarRol(userId, rol)));
    }

    // ── DELETE /api/v1/usuarios/{userId} (soft delete) ───────────────────────
    @DeleteMapping("/api/v1/usuarios/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deshabilitar(@PathVariable UUID userId) {
        usuarioService.deshabilitar(userId);
        return ResponseEntity.noContent().build();
    }
}
