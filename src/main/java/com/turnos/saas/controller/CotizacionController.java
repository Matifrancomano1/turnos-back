package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.CotizacionService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CotizacionResponse>> crear(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @Valid @RequestBody CreateCotizacionRequest request,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Cotización creada. Se notificará al cliente.",
                        cotizacionService.crear(empresaId, turnoId, request, claims)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CotizacionResponse>> obtener(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId) {
        return ResponseEntity.ok(ApiResponse.ok(cotizacionService.obtener(empresaId, turnoId)));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CotizacionResponse>> actualizar(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @Valid @RequestBody UpdateCotizacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cotizacionService.actualizar(empresaId, turnoId, request)));
    }

    @PostMapping("/aceptar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<TurnoResponse>> aceptar(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cotización aceptada. Proceda con el pago de la seña para confirmar su turno.",
                cotizacionService.aceptar(empresaId, turnoId, claims)));
    }

    @PostMapping("/rechazar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<TurnoResponse>> rechazar(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(cotizacionService.rechazar(empresaId, turnoId, claims)));
    }
}
