package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.ServicioMapper;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.Servicio;
import com.turnos.saas.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final EmpresaService empresaService;
    private final ServicioMapper servicioMapper;

    @Transactional(readOnly = true)
    public Page<ServicioResponse> listar(UUID empresaId, boolean soloActivos, Pageable pageable) {
        if (soloActivos) {
            return servicioRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                    .map(servicioMapper::toResponse);
        }
        return servicioRepository.findByEmpresaId(empresaId, pageable)
                .map(servicioMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(UUID empresaId, UUID servicioId) {
        return servicioMapper.toResponse(findOrThrow(empresaId, servicioId));
    }

    @Transactional
    public ServicioResponse crear(UUID empresaId, CreateServicioRequest request) {
        Empresa empresa = empresaService.findActiveOrThrow(empresaId);
        Servicio servicio = Servicio.builder()
                .empresa(empresa)
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .precioBase(request.precioBase())
                .duracionEstimadaMinutos(request.duracionEstimadaMinutos())
                .build();
        servicio = servicioRepository.save(servicio);
        log.info("Servicio creado: id={}, empresaId={}", servicio.getId(), empresaId);
        return servicioMapper.toResponse(servicio);
    }

    @Transactional
    public ServicioResponse actualizar(UUID empresaId, UUID servicioId, UpdateServicioRequest request) {
        Servicio servicio = findOrThrow(empresaId, servicioId);
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setPrecioBase(request.precioBase());
        servicio.setDuracionEstimadaMinutos(request.duracionEstimadaMinutos());
        servicio = servicioRepository.save(servicio);
        log.info("Servicio actualizado: id={}", servicioId);
        return servicioMapper.toResponse(servicio);
    }

    @Transactional
    public ServicioResponse toggleEstado(UUID empresaId, UUID servicioId, boolean activo) {
        Servicio servicio = findOrThrow(empresaId, servicioId);
        servicio.setActivo(activo);
        servicio = servicioRepository.save(servicio);
        log.info("Servicio {} {}: id={}", activo ? "activado" : "desactivado", servicioId, servicioId);
        return servicioMapper.toResponse(servicio);
    }

    public Servicio findOrThrow(UUID empresaId, UUID servicioId) {
        return servicioRepository.findByIdAndEmpresaId(servicioId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio", servicioId));
    }
}
