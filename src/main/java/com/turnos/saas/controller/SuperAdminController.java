package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.CreateTenantAdminRequest;
import com.turnos.saas.dto.request.Requests.UpdateSaasConfigRequest;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.SuperAdminService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador exclusivo del rol SUPER_ADMIN.
 * Todas las rutas bajo /api/v1/superadmin/** están protegidas a nivel de SecurityConfig
 * para requerir el rol SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    // ==============================
    // EMPRESAS
    // ==============================

    /**
     * GET /api/v1/superadmin/empresas
     * Lista todas las empresas del sistema con su número de usuarios.
     */
    @GetMapping("/empresas")
    public ResponseEntity<ApiResponse<List<SuperAdminEmpresaResponse>>> getAllEmpresas() {
        List<SuperAdminEmpresaResponse> empresas = superAdminService.getAllEmpresas();
        return ResponseEntity.ok(ApiResponse.ok("Empresas listadas", empresas));
    }

    /**
     * POST /api/v1/superadmin/empresas
     * Crea una nueva empresa y su primer usuario administrador en una sola transacción.
     */
    @PostMapping("/empresas")
    public ResponseEntity<ApiResponse<EmpresaResponse>> crearEmpresaYAdmin(
            @Valid @RequestBody CreateTenantAdminRequest request) {
        EmpresaResponse response = superAdminService.crearEmpresaYAdmin(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Empresa y administrador creados exitosamente", response));
    }

    // ==============================
    // ESTADÍSTICAS
    // ==============================

    /**
     * GET /api/v1/superadmin/stats
     * Retorna totales globales: empresas, usuarios, turnos.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SuperAdminStatsResponse>> getStats() {
        SuperAdminStatsResponse stats = superAdminService.getStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ==============================
    // IMPERSONATION
    // ==============================

    /**
     * POST /api/v1/superadmin/impersonate/{empresaId}
     * Genera un token JWT temporal (1 hora) con rol ADMIN para la empresa indicada.
     * El JwtAuthFilter pone los Claims como principal del Authentication de Spring Security.
     */
    @PostMapping("/impersonate/{empresaId}")
    public ResponseEntity<ApiResponse<ImpersonationResponse>> impersonate(
            Authentication authentication,
            @PathVariable UUID empresaId) {
        Claims claims = (Claims) authentication.getPrincipal();
        ImpersonationResponse response = superAdminService.impersonate(claims, empresaId);
        return ResponseEntity.ok(ApiResponse.ok("Token de impersonación generado", response));
    }

    // ==============================
    // CONFIGURACIÓN GLOBAL
    // ==============================

    /**
     * GET /api/v1/superadmin/config
     * Obtiene la configuración global de la plataforma SaaS.
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SaasConfigResponse>> getConfig() {
        SaasConfigResponse config = superAdminService.getSaasConfig();
        return ResponseEntity.ok(ApiResponse.ok("Configuración obtenida", config));
    }

    /**
     * PUT /api/v1/superadmin/config
     * Actualiza la configuración global de la plataforma SaaS.
     */
    @PutMapping("/config")
    public ResponseEntity<ApiResponse<SaasConfigResponse>> updateConfig(
            @Valid @RequestBody UpdateSaasConfigRequest request) {
        SaasConfigResponse config = superAdminService.updateSaasConfig(request);
        return ResponseEntity.ok(ApiResponse.ok("Configuración actualizada exitosamente", config));
    }
}
