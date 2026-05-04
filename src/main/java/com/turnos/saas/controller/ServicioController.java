package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.security.TenantGuard;
import com.turnos.saas.service.ServicioService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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

/**
 * CRUD de Servicios con scope de empresa (multi-tenant).
 *
 * Permisos:
 *   - GET (listar, obtener): CLIENTE, OPERADOR, ADMIN
 *   - POST, PUT, PATCH, DELETE: solo ADMIN
 *
 * Todos los endpoints pasan por TenantGuard para garantizar aislamiento.
 */
@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;
    private final TenantGuard tenantGuard;

    /**
     * Lista los servicios de la empresa.
     * Parámetro opcional: soloActivos=true (default) para filtrar inactivos.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ServicioResponse>>> listar(
            @PathVariable UUID empresaId,
            @RequestParam(defaultValue = "true") boolean soloActivos,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        tenantGuard.assertAccess(empresaId, (Claims) request.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(servicioService.listar(empresaId, soloActivos, pageable)));
    }

    /**
     * Obtiene el detalle de un servicio específico.
     */
    @GetMapping("/{servicioId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> obtener(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId,
            HttpServletRequest request) {
        tenantGuard.assertAccess(empresaId, (Claims) request.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(servicioService.obtener(empresaId, servicioId)));
    }

    /**
     * Crea un nuevo servicio para la empresa.
     * Campos: nombre, descripcion, precioBase (BigDecimal), duracionEstimadaMinutos.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> crear(
            @PathVariable UUID empresaId,
            @Valid @RequestBody CreateServicioRequest request,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Servicio creado exitosamente",
                        servicioService.crear(empresaId, request)));
    }

    /**
     * Actualiza completamente un servicio existente (nombre, precio, duración).
     */
    @PutMapping("/{servicioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> actualizar(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId,
            @Valid @RequestBody UpdateServicioRequest request,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(servicioService.actualizar(empresaId, servicioId, request)));
    }

    /**
     * Activa o desactiva un servicio (soft enable/disable).
     * Query param: activo=true|false
     */
    @PatchMapping("/{servicioId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> toggleEstado(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId,
            @RequestParam boolean activo,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(servicioService.toggleEstado(empresaId, servicioId, activo)));
    }

    /**
     * Elimina (soft-delete) un servicio desactivándolo permanentemente.
     * Alternativa semánticamente correcta a PATCH /estado?activo=false.
     */
    @DeleteMapping("/{servicioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID empresaId,
            @PathVariable UUID servicioId,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        servicioService.toggleEstado(empresaId, servicioId, false);
        return ResponseEntity.noContent().build();
    }
}
