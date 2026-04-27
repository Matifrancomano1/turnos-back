package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ServicioResponse>>> listar(
            @PathVariable UUID empresaId,
            @RequestParam(defaultValue = "true") boolean soloActivos,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(servicioService.listar(empresaId, soloActivos, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> crear(
            @PathVariable UUID empresaId,
            @Valid @RequestBody CreateServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Servicio creado exitosamente",
                        servicioService.crear(empresaId, request)));
    }

    @GetMapping("/{servicioId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> obtener(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId) {
        return ResponseEntity.ok(ApiResponse.ok(servicioService.obtener(empresaId, servicioId)));
    }

    @PutMapping("/{servicioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> actualizar(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId,
            @Valid @RequestBody UpdateServicioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(servicioService.actualizar(empresaId, servicioId, request)));
    }

    @PatchMapping("/{servicioId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> toggleEstado(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(ApiResponse.ok(servicioService.toggleEstado(empresaId, servicioId, activo)));
    }
}
