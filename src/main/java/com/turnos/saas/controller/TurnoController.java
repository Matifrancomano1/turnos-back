package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.service.TurnoService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TurnoSummary>>> listar(
            @PathVariable UUID empresaId,
            @RequestParam(required = false) TurnoEstado estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) UUID clienteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.listar(empresaId, estado, fecha, clienteId, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<TurnoResponse>> solicitar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody SolicitarTurnoRequest request,
            @AuthenticationPrincipal Claims claims) {
        TurnoResponse data = turnoService.solicitar(empresaId, request, claims);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Turno solicitado. El operador le enviará una cotización pronto.", data));
    }

    @GetMapping("/{turnoId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TurnoResponse>> detalle(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.detalle(empresaId, turnoId, claims)));
    }

    @PatchMapping("/{turnoId}/estado")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TurnoResponse>> cambiarEstado(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @Valid @RequestBody CambiarEstadoRequest request,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.cambiarEstado(empresaId, turnoId, request, claims)));
    }

    @PutMapping("/{turnoId}/reprogramar")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TurnoResponse>> reprogramar(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @Valid @RequestBody ReprogramarTurnoRequest request,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.reprogramar(empresaId, turnoId, request, claims)));
    }

    @DeleteMapping("/{turnoId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<Void> cancelar(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @RequestParam(required = false) String motivo,
            @AuthenticationPrincipal Claims claims) {
        turnoService.cancelar(empresaId, turnoId, motivo, claims);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{turnoId}/senia")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TurnoResponse>> registrarSenia(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @Valid @RequestBody RegistrarSeniaRequest request,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.registrarSenia(empresaId, turnoId, request, claims)));
    }

    @PostMapping("/{turnoId}/finalizar")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TurnoResponse>> finalizar(
            @PathVariable UUID empresaId,
            @PathVariable UUID turnoId,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.finalizar(empresaId, turnoId, claims)));
    }
}
