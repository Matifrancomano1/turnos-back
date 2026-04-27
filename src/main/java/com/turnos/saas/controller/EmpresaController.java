package com.turnos.saas.controller;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.EmpresaService;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EmpresaResponse>>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.listar(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> crear(@Valid @RequestBody CreateEmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Empresa creada exitosamente", empresaService.crear(request)));
    }

    @GetMapping("/{empresaId}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> obtener(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.obtener(empresaId)));
    }

    @PutMapping("/{empresaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody UpdateEmpresaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.actualizar(empresaId, request)));
    }

    @PatchMapping("/{empresaId}/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmpresaResponse>> actualizarConfig(
            @PathVariable UUID empresaId,
            @Valid @RequestBody UpdateEmpresaConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.actualizarConfig(empresaId, request)));
    }

    @DeleteMapping("/{empresaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deshabilitar(@PathVariable UUID empresaId) {
        empresaService.deshabilitar(empresaId);
        return ResponseEntity.noContent().build();
    }
}
