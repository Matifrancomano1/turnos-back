package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.CreateTenantAdminRequest;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.exception.SlugDuplicadoException;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.NotifConfig;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.repository.EmpresaRepository;
import com.turnos.saas.repository.NotifConfigRepository;
import com.turnos.saas.repository.TurnoRepository;
import com.turnos.saas.repository.UsuarioRepository;
import com.turnos.saas.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotifConfigRepository notifConfigRepository;
    private final TurnoRepository turnoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ==============================
    // QUERIES
    // ==============================

    /**
     * Lista todas las empresas del sistema (activas e inactivas) enriquecidas con
     * el número total de usuarios registrados. Endpoint exclusivo de SuperAdmin.
     */
    @Transactional(readOnly = true)
    public List<SuperAdminEmpresaResponse> getAllEmpresas() {
        return empresaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(e -> new SuperAdminEmpresaResponse(
                        e.getId(),
                        e.getNombre(),
                        e.getSlug(),
                        e.getEmailContacto(),
                        e.getActiva(),
                        e.getCreatedAt(),
                        usuarioRepository.countByEmpresaId(e.getId())
                ))
                .toList();
    }

    /**
     * Retorna estadísticas globales de la plataforma: total de empresas, usuarios y turnos.
     */
    @Transactional(readOnly = true)
    public SuperAdminStatsResponse getStats() {
        long totalEmpresas = empresaRepository.count();
        long totalUsuarios = usuarioRepository.count();
        long totalTurnos   = turnoRepository.count();
        return new SuperAdminStatsResponse(totalEmpresas, totalUsuarios, totalTurnos);
    }

    // ==============================
    // COMMANDS
    // ==============================

    /**
     * Crea una nueva empresa junto con su primer usuario administrador en una sola
     * transacción. Si cualquier paso falla, se hace rollback completo.
     */
    @Transactional
    public EmpresaResponse crearEmpresaYAdmin(CreateTenantAdminRequest request) {
        // Validar slug único
        if (empresaRepository.existsBySlug(request.slug())) {
            throw new SlugDuplicadoException(request.slug());
        }

        // Validar email del admin único
        if (usuarioRepository.existsByEmail(request.adminEmail())) {
            throw new BusinessRuleException("Ya existe una cuenta con ese email: " + request.adminEmail());
        }

        // Crear empresa con config por defecto
        Empresa empresa = Empresa.builder()
                .nombre(request.nombre())
                .emailContacto(request.emailContacto())
                .direccion(request.direccion())
                .telefono(request.telefono())
                .slug(request.slug())
                .horaApertura(LocalTime.of(8, 0))
                .horaCierre(LocalTime.of(18, 0))
                .duracionSlotMinutos(30)
                .sabadoHabilitado(false)
                .domingoHabilitado(false)
                .build();
        empresa = empresaRepository.save(empresa);

        // Crear config de notificaciones vacía
        notifConfigRepository.save(NotifConfig.builder().empresa(empresa).build());

        // Crear usuario admin inicial
        Usuario admin = Usuario.builder()
                .empresa(empresa)
                .nombre(request.adminNombre())
                .email(request.adminEmail())
                .passwordHash(passwordEncoder.encode(request.adminPassword()))
                .telefono(request.adminTelefono())
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        usuarioRepository.save(admin);

        log.info("SuperAdmin: empresa creada id={}, slug={}, admin={}", empresa.getId(), empresa.getSlug(), admin.getEmail());

        return new EmpresaResponse(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getSlug(),
                empresa.getEmailContacto(),
                empresa.getDireccion(),
                empresa.getTelefono(),
                new EmpresaConfigResponse(
                        empresa.getHoraApertura(),
                        empresa.getHoraCierre(),
                        empresa.getDuracionSlotMinutos(),
                        empresa.getSabadoHabilitado(),
                        empresa.getDomingoHabilitado()
                ),
                empresa.getActiva(),
                empresa.getCreatedAt()
        );
    }

    /**
     * Genera un token de impersonación temporal (1 hora) con rol ADMIN para la
     * empresa indicada. El subject del token es el ID del SuperAdmin que lo solicita.
     *
     * @param claims     Claims del token JWT del SuperAdmin autenticado
     * @param empresaId  ID de la empresa a impersonar
     */
    @Transactional(readOnly = true)
    public ImpersonationResponse impersonate(Claims claims, UUID empresaId) {
        // Verificar que la empresa exista (activa o no, el SuperAdmin puede verla)
        empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", empresaId));

        UUID adminUserId = jwtUtil.extractUserId(claims);
        String token = jwtUtil.generateImpersonationToken(adminUserId, empresaId);

        log.warn("SuperAdmin impersonation: adminId={} → empresaId={}", adminUserId, empresaId);

        return new ImpersonationResponse(token, 3_600L, empresaId);
    }
}
