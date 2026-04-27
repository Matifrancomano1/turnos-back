package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.InvalidStateTransitionException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.CotizacionMapper;
import com.turnos.saas.mapper.TurnoMapper;
import com.turnos.saas.model.entity.*;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.*;
import com.turnos.saas.security.JwtUtil;
import com.turnos.saas.security.TenantGuard;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final TurnoHistorialRepository historialRepository;
    private final CotizacionRepository cotizacionRepository;
    private final SeniaRepository seniaRepository;
    private final EmpresaService empresaService;
    private final ServicioService servicioService;
    private final UsuarioService usuarioService;
    private final TurnoMapper turnoMapper;
    private final CotizacionMapper cotizacionMapper;
    private final JwtUtil jwtUtil;
    private final TenantGuard tenantGuard;
    private final NotificacionService notificacionService;

    @Transactional
    public TurnoResponse solicitar(UUID empresaId, SolicitarTurnoRequest request, Claims claims) {
        tenantGuard.assertAccess(empresaId, claims);
        UUID clienteId = jwtUtil.extractUserId(claims);

        Empresa empresa = empresaService.findActiveOrThrow(empresaId);
        Usuario cliente = usuarioService.findByIdOrThrow(clienteId);
        Servicio servicio = servicioService.findOrThrow(empresaId, request.servicioId());

        Turno turno = Turno.builder()
                .empresa(empresa)
                .cliente(cliente)
                .servicio(servicio)
                .fechaSolicitada(request.fechaPreferida())
                .horaSolicitada(request.horaPreferida())
                .observaciones(request.observaciones())
                .estado(TurnoEstado.SOLICITADO)
                .build();

        turno = turnoRepository.save(turno);
        registrarHistorial(turno, null, TurnoEstado.SOLICITADO, null, cliente);
        log.info("Turno solicitado: id={}, clienteId={}, empresaId={}", turno.getId(), clienteId, empresaId);
        return buildTurnoResponse(turno);
    }

    @Transactional(readOnly = true)
    public Page<TurnoSummary> listar(UUID empresaId, TurnoEstado estado, LocalDate fecha, UUID clienteId, Pageable pageable) {
        return turnoRepository.findByFilters(empresaId, estado, fecha, clienteId, pageable)
                .map(turnoMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public TurnoResponse detalle(UUID empresaId, UUID turnoId, Claims claims) {
        Turno turno = findTurnoOrThrow(empresaId, turnoId);
        UUID tokenUserId = jwtUtil.extractUserId(claims);
        Rol tokenRol = jwtUtil.extractRol(claims);

        if (tokenRol == Rol.CLIENTE && !turno.getCliente().getId().equals(tokenUserId)) {
            throw new ResourceNotFoundException("Turno", turnoId);
        }
        return buildTurnoResponse(turno);
    }

    @Transactional
    public TurnoResponse cambiarEstado(UUID empresaId, UUID turnoId, CambiarEstadoRequest request, Claims claims) {
        Turno turno = findTurnoOrThrow(empresaId, turnoId);
        UUID userId = jwtUtil.extractUserId(claims);
        Usuario operador = usuarioService.findByIdOrThrow(userId);

        transicionarEstado(turno, request.nuevoEstado(), request.motivo(), operador);
        turno = turnoRepository.save(turno);
        notificacionService.notificarTransicion(turnoId, request.nuevoEstado(), empresaId);
        log.info("Estado turno cambiado: id={}, nuevo={}", turnoId, request.nuevoEstado());
        return buildTurnoResponse(turno);
    }

    @Transactional
    public TurnoResponse reprogramar(UUID empresaId, UUID turnoId, ReprogramarTurnoRequest request, Claims claims) {
        Turno turno = findTurnoOrThrow(empresaId, turnoId);
        UUID userId = jwtUtil.extractUserId(claims);
        Usuario operador = usuarioService.findByIdOrThrow(userId);

        turno.setFechaConfirmada(request.nuevaFecha());
        turno.setHoraConfirmada(request.nuevaHora());
        turno = turnoRepository.save(turno);

        registrarHistorial(turno, turno.getEstado(), turno.getEstado(), "Reprogramación: " + request.motivo(), operador);
        notificacionService.notificarReprogramacion(turnoId, empresaId);
        log.info("Turno reprogramado: id={}", turnoId);
        return buildTurnoResponse(turno);
    }

    @Transactional
    public void cancelar(UUID empresaId, UUID turnoId, String motivo, Claims claims) {
        Turno turno = findTurnoOrThrow(empresaId, turnoId);
        UUID userId = jwtUtil.extractUserId(claims);
        Rol rol = jwtUtil.extractRol(claims);
        Usuario usuario = usuarioService.findByIdOrThrow(userId);

        boolean esCliente = rol == Rol.CLIENTE;
        if (esCliente) {
            if (!turno.getCliente().getId().equals(userId)) {
                throw new ResourceNotFoundException("Turno", turnoId);
            }
            if (turno.getFechaConfirmada() != null && turno.getHoraConfirmada() != null) {
                LocalDateTime turnoDateTime = LocalDateTime.of(turno.getFechaConfirmada(), turno.getHoraConfirmada());
                long horasRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), turnoDateTime);
                if (horasRestantes < 48) {
                    throw new BusinessRuleException(
                            "El cliente no puede cancelar con menos de 48 horas de anticipación");
                }
            }
        }

        transicionarEstado(turno, TurnoEstado.CANCELADO, motivo, usuario);
        turnoRepository.save(turno);
        notificacionService.notificarTransicion(turnoId, TurnoEstado.CANCELADO, empresaId);
        log.info("Turno cancelado: id={}, porRol={}", turnoId, rol);
    }

    @Transactional
    public TurnoResponse registrarSenia(UUID empresaId, UUID turnoId, RegistrarSeniaRequest request, Claims claims) {
        Turno turno = findTurnoOrThrow(empresaId, turnoId);

        if (seniaRepository.existsByTurnoId(turnoId)) {
            throw new BusinessRuleException("Este turno ya tiene una seña registrada");
        }

        Senia senia = Senia.builder()
                .turno(turno)
                .monto(request.monto())
                .metodoPago(request.metodoPago())
                .referencia(request.referencia())
                .build();
        seniaRepository.save(senia);

        if (turno.getEstado() == TurnoEstado.CONFIRMADO) {
            transicionarEstado(turno, TurnoEstado.PROGRAMADO, "Seña registrada", null);
            turnoRepository.save(turno);
        }

        log.info("Seña registrada para turnoId={}, monto={}", turnoId, request.monto());
        return buildTurnoResponse(turno);
    }

    @Transactional
    public TurnoResponse finalizar(UUID empresaId, UUID turnoId, Claims claims) {
        Turno turno = findTurnoOrThrow(empresaId, turnoId);
        UUID userId = jwtUtil.extractUserId(claims);
        Usuario operador = usuarioService.findByIdOrThrow(userId);

        transicionarEstado(turno, TurnoEstado.FINALIZADO, null, operador);
        turno = turnoRepository.save(turno);
        notificacionService.notificarTransicion(turnoId, TurnoEstado.FINALIZADO, empresaId);
        log.info("Turno finalizado: id={}", turnoId);
        return buildTurnoResponse(turno);
    }

    @Transactional(readOnly = true)
    public Page<TurnoSummary> missTurnos(Claims claims, Pageable pageable) {
        UUID clienteId = jwtUtil.extractUserId(claims);
        return turnoRepository.findByClienteId(clienteId, pageable).map(turnoMapper::toSummary);
    }

    private void transicionarEstado(Turno turno, TurnoEstado nuevoEstado, String motivo, Usuario cambiadoPor) {
        TurnoEstado estadoActual = turno.getEstado();
        if (!estadoActual.canTransitionTo(nuevoEstado)) {
            throw new InvalidStateTransitionException(estadoActual, nuevoEstado);
        }
        turno.setEstado(nuevoEstado);
        registrarHistorial(turno, estadoActual, nuevoEstado, motivo, cambiadoPor);
    }

    private void registrarHistorial(Turno turno, TurnoEstado anterior, TurnoEstado nuevo, String motivo, Usuario cambiadoPor) {
        TurnoHistorial historial = TurnoHistorial.builder()
                .turno(turno)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .motivo(motivo)
                .cambiadoPor(cambiadoPor)
                .build();
        historialRepository.save(historial);
    }

    public TurnoResponse buildTurnoResponse(Turno turno) {
        TurnoResponse base = turnoMapper.toResponse(turno);

        CotizacionInfo cotizacionInfo = cotizacionRepository.findByTurnoId(turno.getId())
                .map(cotizacionMapper::toInfo).orElse(null);

        SeniaInfo seniaInfo = seniaRepository.findByTurnoId(turno.getId())
                .map(s -> new SeniaInfo(s.getId(), s.getMonto(), s.getMetodoPago(), s.getReferencia(), s.getRegistradoEn()))
                .orElse(null);

        List<TurnoHistorialEntry> historial = turnoMapper.toHistorialEntryList(
                historialRepository.findByTurnoIdOrderByTimestampAsc(turno.getId())
        );

        return new TurnoResponse(
                base.id(), base.empresaId(), base.cliente(), base.servicio(),
                base.fechaSolicitada(), base.horaSolicitada(),
                base.fechaConfirmada(), base.horaConfirmada(),
                base.estado(), base.observaciones(),
                cotizacionInfo, seniaInfo, historial, base.creadoEn()
        );
    }

    public Turno findTurnoOrThrow(UUID empresaId, UUID turnoId) {
        return turnoRepository.findByIdAndEmpresaId(turnoId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", turnoId));
    }
}
