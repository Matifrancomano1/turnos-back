package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.CotizacionMapper;
import com.turnos.saas.model.entity.Cotizacion;
import com.turnos.saas.model.entity.Servicio;
import com.turnos.saas.model.entity.Turno;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.CotizacionEstado;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.CotizacionRepository;
import com.turnos.saas.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final TurnoService turnoService;
    private final ServicioService servicioService;
    private final UsuarioService usuarioService;
    private final CotizacionMapper cotizacionMapper;
    private final JwtUtil jwtUtil;
    private final NotificacionService notificacionService;

    @Transactional
    public CotizacionResponse crear(UUID empresaId, UUID turnoId, CreateCotizacionRequest request, Claims claims) {
        Turno turno = turnoService.findTurnoOrThrow(empresaId, turnoId);

        if (turno.getEstado() != TurnoEstado.EN_COTIZACION) {
            throw new BusinessRuleException("El turno debe estar en estado EN_COTIZACION para cotizar");
        }
        if (cotizacionRepository.findByTurnoId(turnoId).isPresent()) {
            throw new BusinessRuleException("Este turno ya tiene una cotización");
        }

        Servicio servicio = request.servicioId() != null
                ? servicioService.findOrThrow(empresaId, request.servicioId())
                : turno.getServicio();

        Cotizacion cotizacion = Cotizacion.builder()
                .turno(turno)
                .servicio(servicio)
                .precio(request.precio())
                .duracionMinutos(request.duracionMinutos())
                .descripcion(request.descripcion())
                .estado(CotizacionEstado.PENDIENTE)
                .build();

        cotizacion = cotizacionRepository.save(cotizacion);

        UUID operadorId = jwtUtil.extractUserId(claims);
        Usuario operador = usuarioService.findByIdOrThrow(operadorId);
        turnoService.cambiarEstado(empresaId, turnoId,
                new CambiarEstadoRequest(TurnoEstado.COTIZADO, "Cotización creada"), claims);

        notificacionService.notificarTransicion(turnoId, TurnoEstado.COTIZADO, empresaId);
        log.info("Cotización creada: id={}, turnoId={}", cotizacion.getId(), turnoId);
        return cotizacionMapper.toResponse(cotizacion);
    }

    @Transactional(readOnly = true)
    public CotizacionResponse obtener(UUID empresaId, UUID turnoId) {
        turnoService.findTurnoOrThrow(empresaId, turnoId);
        Cotizacion cotizacion = cotizacionRepository.findByTurnoId(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización para turno", turnoId));
        return cotizacionMapper.toResponse(cotizacion);
    }

    @Transactional
    public CotizacionResponse actualizar(UUID empresaId, UUID turnoId, UpdateCotizacionRequest request) {
        turnoService.findTurnoOrThrow(empresaId, turnoId);
        Cotizacion cotizacion = cotizacionRepository.findByTurnoId(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización para turno", turnoId));

        if (cotizacion.getEstado() != CotizacionEstado.PENDIENTE) {
            throw new BusinessRuleException("Solo se puede modificar una cotización en estado PENDIENTE");
        }

        cotizacion.setPrecio(request.precio());
        cotizacion.setDuracionMinutos(request.duracionMinutos());
        cotizacion.setDescripcion(request.descripcion());
        cotizacion = cotizacionRepository.save(cotizacion);
        log.info("Cotización actualizada: turnoId={}", turnoId);
        return cotizacionMapper.toResponse(cotizacion);
    }

    @Transactional
    public TurnoResponse aceptar(UUID empresaId, UUID turnoId, Claims claims) {
        Turno turno = turnoService.findTurnoOrThrow(empresaId, turnoId);
        Cotizacion cotizacion = cotizacionRepository.findByTurnoId(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización para turno", turnoId));

        if (cotizacion.getEstado() != CotizacionEstado.PENDIENTE) {
            throw new BusinessRuleException("La cotización no está en estado PENDIENTE");
        }

        cotizacion.setEstado(CotizacionEstado.ACEPTADA);
        cotizacionRepository.save(cotizacion);

        turnoService.cambiarEstado(empresaId, turnoId,
                new CambiarEstadoRequest(TurnoEstado.CONFIRMADO, "Cotización aceptada por el cliente"), claims);

        notificacionService.notificarTransicion(turnoId, TurnoEstado.CONFIRMADO, empresaId);
        log.info("Cotización aceptada: turnoId={}", turnoId);
        return turnoService.buildTurnoResponse(turno);
    }

    @Transactional
    public TurnoResponse rechazar(UUID empresaId, UUID turnoId, Claims claims) {
        Turno turno = turnoService.findTurnoOrThrow(empresaId, turnoId);
        Cotizacion cotizacion = cotizacionRepository.findByTurnoId(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización para turno", turnoId));

        if (cotizacion.getEstado() != CotizacionEstado.PENDIENTE) {
            throw new BusinessRuleException("La cotización no está en estado PENDIENTE");
        }

        cotizacion.setEstado(CotizacionEstado.RECHAZADA);
        cotizacionRepository.save(cotizacion);

        turnoService.cambiarEstado(empresaId, turnoId,
                new CambiarEstadoRequest(TurnoEstado.CANCELADO, "Cotización rechazada por el cliente"), claims);

        notificacionService.notificarTransicion(turnoId, TurnoEstado.CANCELADO, empresaId);
        log.info("Cotización rechazada: turnoId={}", turnoId);
        return turnoService.buildTurnoResponse(turno);
    }
}
