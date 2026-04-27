package com.turnos.saas.controller;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas/{empresaId}/reportes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(
            @PathVariable UUID empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(reporteService.getDashboard(empresaId, desde, hasta)));
    }

    @GetMapping("/turnos")
    public ResponseEntity<ApiResponse<Page<TurnoSummary>>> turnos(
            @PathVariable UUID empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reporteService.getTurnos(empresaId, desde, hasta, pageable)));
    }

    @GetMapping("/servicios")
    public ResponseEntity<ApiResponse<List<TopServicioItem>>> servicios(
            @PathVariable UUID empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(reporteService.getTopServicios(empresaId, desde, hasta)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) throws IOException {
        byte[] xlsx = reporteService.exportExcel(empresaId, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-" + desde + "-" + hasta + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
