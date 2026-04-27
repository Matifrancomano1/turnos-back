package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.NotificacionService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/api/v1/usuarios/me/notificaciones")
    public ResponseEntity<ApiResponse<Page<NotificacionResponse>>> listar(
            @AuthenticationPrincipal Claims claims,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(notificacionService.listarMias(claims, pageable)));
    }

    @PatchMapping("/api/v1/usuarios/me/notificaciones/{id}/leer")
    public ResponseEntity<Void> marcarLeida(
            @PathVariable UUID id,
            @AuthenticationPrincipal Claims claims) {
        notificacionService.marcarLeida(id, claims);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/empresas/{empresaId}/notificaciones/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotifConfigResponse>> getConfig(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(ApiResponse.ok(notificacionService.getConfig(empresaId)));
    }

    @PutMapping("/api/v1/empresas/{empresaId}/notificaciones/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotifConfigResponse>> actualizarConfig(
            @PathVariable UUID empresaId,
            @Valid @RequestBody UpdateNotifConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(notificacionService.actualizarConfig(empresaId, request)));
    }

    @PostMapping("/api/v1/empresas/{empresaId}/notificaciones/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> test(
            @PathVariable UUID empresaId,
            @RequestParam String canal) {
        notificacionService.enviarTest(empresaId, canal);
        return ResponseEntity.accepted().body(ApiResponse.ok("Notificación de prueba enviada", null));
    }
}
