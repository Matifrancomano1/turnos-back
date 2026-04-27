package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.AgendaService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DisponibilidadResponse>> disponibilidad(
            @PathVariable UUID empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) UUID servicioId) {
        return ResponseEntity.ok(ApiResponse.ok(agendaService.calcularDisponibilidad(empresaId, fecha, servicioId)));
    }

    @GetMapping("/bloqueos")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BloqueoResponse>>> listarBloqueos(
            @PathVariable UUID empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(agendaService.listarBloqueos(empresaId, desde, hasta)));
    }

    @PostMapping("/bloqueos")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<BloqueoResponse>> crearBloqueo(
            @PathVariable UUID empresaId,
            @Valid @RequestBody CreateBloqueoRequest request,
            @AuthenticationPrincipal Claims claims) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Bloqueo creado exitosamente",
                        agendaService.crearBloqueo(empresaId, request, claims)));
    }

    @DeleteMapping("/bloqueos/{bloqueoId}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<Void> eliminarBloqueo(
            @PathVariable UUID empresaId,
            @PathVariable UUID bloqueoId) {
        agendaService.eliminarBloqueo(empresaId, bloqueoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendario")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CalendarioResponse>> calendario(
            @PathVariable UUID empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "DIA") String vista) {
        return ResponseEntity.ok(ApiResponse.ok(agendaService.getCalendario(empresaId, fecha, vista)));
    }
}
