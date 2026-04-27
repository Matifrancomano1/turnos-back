package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.EmpresaMapper;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.repository.EmpresaRepository;
import com.turnos.saas.repository.NotifConfigRepository;
import com.turnos.saas.model.entity.NotifConfig;
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

    @Transactional(readOnly = true)
    public Page<EmpresaResponse> listar(Pageable pageable) {
        return empresaRepository.findAll(pageable).map(empresaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EmpresaResponse obtener(UUID id) {
        return empresaMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    public EmpresaResponse crear(CreateEmpresaRequest request) {
        Empresa empresa = Empresa.builder()
                .nombre(request.nombre())
                .emailContacto(request.emailContacto())
                .direccion(request.direccion())
                .telefono(request.telefono())
                .horaApertura(LocalTime.of(8, 0))
                .horaCierre(LocalTime.of(18, 0))
                .duracionSlotMinutos(30)
                .sabadoHabilitado(false)
                .domingoHabilitado(false)
                .build();
        empresa = empresaRepository.save(empresa);

        NotifConfig config = NotifConfig.builder()
                .empresa(empresa)
                .build();
        notifConfigRepository.save(config);

        log.info("Empresa creada: id={}, nombre={}", empresa.getId(), empresa.getNombre());
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public EmpresaResponse actualizar(UUID id, UpdateEmpresaRequest request) {
        Empresa empresa = findActiveOrThrow(id);
        empresa.setNombre(request.nombre());
        empresa.setEmailContacto(request.emailContacto());
        empresa.setDireccion(request.direccion());
        empresa.setTelefono(request.telefono());
        empresa = empresaRepository.save(empresa);
        log.info("Empresa actualizada: id={}", id);
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public EmpresaResponse actualizarConfig(UUID id, UpdateEmpresaConfigRequest request) {
        Empresa empresa = findActiveOrThrow(id);
        empresa.setHoraApertura(request.horaApertura());
        empresa.setHoraCierre(request.horaCierre());
        empresa.setDuracionSlotMinutos(request.duracionSlotMinutos());
        empresa.setSabadoHabilitado(request.sabadoHabilitado());
        empresa.setDomingoHabilitado(request.domingoHabilitado());
        empresa = empresaRepository.save(empresa);
        log.info("Config empresa actualizada: id={}", id);
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public void deshabilitar(UUID id) {
        Empresa empresa = findActiveOrThrow(id);
        empresa.setActiva(false);
        empresaRepository.save(empresa);
        log.info("Empresa deshabilitada (soft delete): id={}", id);
    }

    public Empresa findActiveOrThrow(UUID id) {
        return empresaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
    }
}
