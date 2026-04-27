package com.turnos.saas.controller;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.service.TurnoService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final TurnoService turnoService;

    @GetMapping("/me/turnos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<Page<TurnoSummary>>> misTurnos(
            @AuthenticationPrincipal Claims claims,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(turnoService.missTurnos(claims, pageable)));
    }
}
