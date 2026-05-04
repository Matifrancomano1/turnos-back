package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.security.TenantGuard;
import com.turnos.saas.service.EmpresaService;
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

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;
    private final TenantGuard tenantGuard;

    // ==============================
    // ADMIN-ONLY: Gestión global
    // ==============================

    /**
     * Lista todas las empresas. Solo el super-admin del SaaS tiene acceso.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EmpresaResponse>>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.listar(pageable)));
    }

    /**
     * Crea una nueva empresa (tenant). Solo el super-admin del SaaS.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> crear(@Valid @RequestBody CreateEmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Empresa creada exitosamente", empresaService.crear(request)));
    }

    // ==============================
    // Scoped: Acceso por empresaId (con TenantGuard)
    // ==============================

    /**
     * Obtiene el detalle de una empresa (incluyendo su config de horarios).
     * Accesible por OPERADOR y ADMIN. El TenantGuard asegura que el OPERADOR
     * solo pueda ver SU propia empresa.
     */
    @GetMapping("/{empresaId}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtener(
            @PathVariable UUID empresaId,
            HttpServletRequest request) {
        tenantGuard.assertAccess(empresaId, (Claims) request.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(empresaService.obtener(empresaId)));
    }

    /**
     * Actualiza los datos de contacto/nombre y el slug de la empresa.
     * Restringido a ADMIN. TenantGuard previene edición cruzada entre empresas.
     */
    @PutMapping("/{empresaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody UpdateEmpresaRequest request,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(empresaService.actualizar(empresaId, request)));
    }

    /**
     * Actualiza la configuración de horarios y slots de la empresa.
     * Solo ADMIN. Soporta valores: 15, 30, 45, 60 minutos.
     * El servicio valida que sea múltiplo de 15 y que horaCierre &gt; horaApertura.
     */
    @PatchMapping("/{empresaId}/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizarConfig(
            @PathVariable UUID empresaId,
            @Valid @RequestBody UpdateEmpresaConfigRequest request,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        return ResponseEntity.ok(ApiResponse.ok(empresaService.actualizarConfig(empresaId, request)));
    }

    /**
     * Deshabilita (soft-delete) una empresa. Solo el super-admin del SaaS.
     */
    @DeleteMapping("/{empresaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deshabilitar(
            @PathVariable UUID empresaId,
            HttpServletRequest httpRequest) {
        tenantGuard.assertAccess(empresaId, (Claims) httpRequest.getAttribute("claims"));
        empresaService.deshabilitar(empresaId);
        return ResponseEntity.noContent().build();
    }

    // ==============================
    // PUBLIC: Lookup por slug
    // ==============================

    /**
     * Endpoint público (sin autenticación) para obtener los datos básicos
     * de una empresa por su slug. Útil para la página de reservas del cliente.
     * Ejemplo: GET /api/v1/empresas/slug/mi-taller-mecanico
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.obtenerPorSlug(slug)));
    }
}
