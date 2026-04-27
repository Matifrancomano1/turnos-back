package com.turnos.saas.security;

import com.turnos.saas.exception.TenantAccessException;
import com.turnos.saas.model.enums.Rol;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantGuard {

    private final JwtUtil jwtUtil;

    public void assertAccess(UUID empresaId, Claims claims) {
        UUID tokenEmpresaId = jwtUtil.extractEmpresaId(claims);
        Rol rol = jwtUtil.extractRol(claims);

        if (rol == Rol.ADMIN && tokenEmpresaId == null) {
            return;
        }

        if (tokenEmpresaId == null || !tokenEmpresaId.equals(empresaId)) {
            UUID userId = jwtUtil.extractUserId(claims);
            log.warn("Tenant access violation: userId={} attempted access to empresaId={} (token empresaId={})",
                    userId, empresaId, tokenEmpresaId);
            throw new TenantAccessException(
                    "No tiene permiso para acceder a los recursos de esta empresa");
        }
    }

    public void assertResourceBelongsToTenant(UUID resourceEmpresaId, UUID requestedEmpresaId) {
        if (!resourceEmpresaId.equals(requestedEmpresaId)) {
            throw new TenantAccessException(
                    "El recurso solicitado no pertenece a esta empresa");
        }
    }
}
