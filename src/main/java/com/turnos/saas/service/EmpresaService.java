package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.exception.SlugDuplicadoException;
import com.turnos.saas.mapper.EmpresaMapper;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.NotifConfig;
import com.turnos.saas.repository.EmpresaRepository;
import com.turnos.saas.repository.NotifConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final NotifConfigRepository notifConfigRepository;
    private final EmpresaMapper empresaMapper;

    // ==============================
    // QUERIES
    // ==============================

    @Transactional(readOnly = true)
    public Page<EmpresaResponse> listar(Pageable pageable) {
        return empresaRepository.findAll(pageable).map(empresaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EmpresaResponse obtener(UUID id) {
        return empresaMapper.toResponse(findActiveOrThrow(id));
    }

    /**
     * Obtiene una empresa por su slug público.
     * Endpoint pensado para el acceso público de clientes via URL amigable.
     */
    @Transactional(readOnly = true)
    public EmpresaResponse obtenerPorSlug(String slug) {
        Empresa empresa = empresaRepository.findBySlugAndActivaTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa con slug '" + slug + "' no encontrada"));
        return empresaMapper.toResponse(empresa);
    }

    // ==============================
    // COMMANDS
    // ==============================

    @Transactional
    public EmpresaResponse crear(CreateEmpresaRequest request) {
        // Validar unicidad del slug antes de persistir
        if (empresaRepository.existsBySlug(request.slug())) {
            throw new SlugDuplicadoException(request.slug());
        }

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

        // Crear config de notificaciones vacía al mismo tiempo
        NotifConfig config = NotifConfig.builder()
                .empresa(empresa)
                .build();
        notifConfigRepository.save(config);

        log.info("Empresa creada: id={}, nombre={}, slug={}", empresa.getId(), empresa.getNombre(), empresa.getSlug());
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public EmpresaResponse actualizar(UUID id, UpdateEmpresaRequest request) {
        Empresa empresa = findActiveOrThrow(id);

        // Validar que el nuevo slug no esté en uso por OTRA empresa
        if (empresaRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new SlugDuplicadoException(request.slug());
        }

        empresa.setNombre(request.nombre());
        empresa.setEmailContacto(request.emailContacto());
        empresa.setDireccion(request.direccion());
        empresa.setTelefono(request.telefono());
        empresa.setSlug(request.slug());
        empresa = empresaRepository.save(empresa);
        log.info("Empresa actualizada: id={}, slug={}", id, empresa.getSlug());
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public EmpresaResponse actualizarConfig(UUID id, UpdateEmpresaConfigRequest request) {
        Empresa empresa = findActiveOrThrow(id);

        // Validar que horaCierre sea después de horaApertura
        if (!request.horaCierre().isAfter(request.horaApertura())) {
            throw new BusinessRuleException(
                    "La hora de cierre debe ser posterior a la hora de apertura");
        }

        // Validar que duracionSlotMinutos sea múltiplo de 15 (15, 30, 45, 60...)
        if (request.duracionSlotMinutos() % 15 != 0) {
            throw new BusinessRuleException(
                    "La duración del slot debe ser múltiplo de 15 minutos (15, 30, 45, 60)");
        }

        empresa.setHoraApertura(request.horaApertura());
        empresa.setHoraCierre(request.horaCierre());
        empresa.setDuracionSlotMinutos(request.duracionSlotMinutos());
        empresa.setSabadoHabilitado(request.sabadoHabilitado());
        empresa.setDomingoHabilitado(request.domingoHabilitado());
        empresa = empresaRepository.save(empresa);
        log.info("Config empresa actualizada: id={}, apertura={}, cierre={}, slotMin={}",
                id, request.horaApertura(), request.horaCierre(), request.duracionSlotMinutos());
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public void deshabilitar(UUID id) {
        Empresa empresa = findActiveOrThrow(id);
        empresa.setActiva(false);
        empresaRepository.save(empresa);
        log.info("Empresa deshabilitada (soft delete): id={}", id);
    }

    // ==============================
    // HELPERS
    // ==============================

    /**
     * Busca una empresa activa por ID o lanza ResourceNotFoundException.
     * Método package-visible para ser usado por otros servicios (ej: ServicioService).
     */
    public Empresa findActiveOrThrow(UUID id) {
        return empresaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
    }
}
