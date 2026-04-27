package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.TurnoMapper;
import com.turnos.saas.model.entity.BloqueoAgenda;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.Servicio;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.BloqueoAgendaRepository;
import com.turnos.saas.repository.TurnoRepository;
import com.turnos.saas.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaService {

    private final BloqueoAgendaRepository bloqueoRepository;
    private final TurnoRepository turnoRepository;
    private final EmpresaService empresaService;
    private final ServicioService servicioService;
    private final UsuarioService usuarioService;
    private final TurnoMapper turnoMapper;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public DisponibilidadResponse calcularDisponibilidad(UUID empresaId, LocalDate fecha, UUID servicioId) {
        Empresa empresa = empresaService.findActiveOrThrow(empresaId);

        int duracionServicio = servicioId != null
                ? servicioService.findOrThrow(empresaId, servicioId).getDuracionEstimadaMinutos()
                : empresa.getDuracionSlotMinutos();

        int slotsNecesarios = (int) Math.ceil((double) duracionServicio / empresa.getDuracionSlotMinutos());

        List<LocalTime> todosLosSlots = generarSlots(
                empresa.getHoraApertura(), empresa.getHoraCierre(), empresa.getDuracionSlotMinutos()
        );

        List<LocalTime> slotsOcupados = turnoRepository
                .findByEmpresaIdAndEstadoInAndFechaConfirmada(
                        empresaId,
                        List.of(TurnoEstado.CONFIRMADO, TurnoEstado.PROGRAMADO),
                        fecha
                )
                .stream()
                .map(t -> t.getHoraConfirmada())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<BloqueoAgenda> bloqueos = bloqueoRepository.findByEmpresaIdAndFecha(empresaId, fecha);

        List<SlotDisponibilidad> resultado = new ArrayList<>();
        for (int i = 0; i < todosLosSlots.size(); i++) {
            LocalTime slot = todosLosSlots.get(i);
            boolean ocupado = slotsOcupados.contains(slot);
            boolean bloqueado = bloqueos.stream().anyMatch(b ->
                    !slot.isBefore(b.getHoraInicio()) && slot.isBefore(b.getHoraFin()));

            boolean hayEspacio = true;
            if (!ocupado && !bloqueado && slotsNecesarios > 1) {
                for (int j = 1; j < slotsNecesarios && (i + j) < todosLosSlots.size(); j++) {
                    LocalTime futuroSlot = todosLosSlots.get(i + j);
                    if (slotsOcupados.contains(futuroSlot) ||
                        bloqueos.stream().anyMatch(b -> !futuroSlot.isBefore(b.getHoraInicio()) && futuroSlot.isBefore(b.getHoraFin()))) {
                        hayEspacio = false;
                        break;
                    }
                }
            }

            resultado.add(new SlotDisponibilidad(fecha, slot, !ocupado && !bloqueado && hayEspacio));
        }

        return new DisponibilidadResponse(fecha, resultado);
    }

    @Transactional(readOnly = true)
    public List<BloqueoResponse> listarBloqueos(UUID empresaId, LocalDate desde, LocalDate hasta) {
        return bloqueoRepository.findByEmpresaIdAndFechaBetween(empresaId, desde, hasta)
                .stream()
                .map(this::toBloqueoResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BloqueoResponse crearBloqueo(UUID empresaId, CreateBloqueoRequest request, Claims claims) {
        Empresa empresa = empresaService.findActiveOrThrow(empresaId);
        UUID userId = jwtUtil.extractUserId(claims);
        Usuario creadoPor = usuarioService.findByIdOrThrow(userId);

        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new BusinessRuleException("La hora de inicio debe ser anterior a la hora de fin");
        }

        BloqueoAgenda bloqueo = BloqueoAgenda.builder()
                .empresa(empresa)
                .fecha(request.fecha())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .motivo(request.motivo())
                .creadoPor(creadoPor)
                .build();

        bloqueo = bloqueoRepository.save(bloqueo);
        log.info("Bloqueo creado: id={}, empresaId={}, fecha={}", bloqueo.getId(), empresaId, request.fecha());
        return toBloqueoResponse(bloqueo);
    }

    @Transactional
    public void eliminarBloqueo(UUID empresaId, UUID bloqueoId) {
        BloqueoAgenda bloqueo = bloqueoRepository.findById(bloqueoId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo", bloqueoId));
        if (!bloqueo.getEmpresa().getId().equals(empresaId)) {
            throw new ResourceNotFoundException("Bloqueo", bloqueoId);
        }
        bloqueoRepository.delete(bloqueo);
        log.info("Bloqueo eliminado: id={}", bloqueoId);
    }

    @Transactional(readOnly = true)
    public CalendarioResponse getCalendario(UUID empresaId, LocalDate fecha, String vista) {
        LocalDate hasta = "SEMANA".equalsIgnoreCase(vista) ? fecha.plusDays(6) : fecha;

        List<TurnoSummary> turnos = new ArrayList<>();
        for (LocalDate d = fecha; !d.isAfter(hasta); d = d.plusDays(1)) {
            turnoRepository.findByEmpresaIdAndEstadoInAndFechaConfirmada(
                    empresaId,
                    List.of(TurnoEstado.CONFIRMADO, TurnoEstado.PROGRAMADO, TurnoEstado.FINALIZADO),
                    d
            ).stream().map(turnoMapper::toSummary).forEach(turnos::add);
        }

        List<BloqueoResponse> bloqueos = bloqueoRepository
                .findByEmpresaIdAndFechaBetween(empresaId, fecha, hasta)
                .stream().map(this::toBloqueoResponse).collect(Collectors.toList());

        return new CalendarioResponse(fecha, turnos, bloqueos);
    }

    private List<LocalTime> generarSlots(LocalTime inicio, LocalTime fin, int duracionMinutos) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = inicio;
        while (current.isBefore(fin)) {
            slots.add(current);
            current = current.plusMinutes(duracionMinutos);
        }
        return slots;
    }

    private BloqueoResponse toBloqueoResponse(BloqueoAgenda b) {
        return new BloqueoResponse(
                b.getId(), b.getFecha(), b.getHoraInicio(), b.getHoraFin(),
                b.getMotivo(),
                b.getCreadoPor() != null ? b.getCreadoPor().getNombre() : null,
                b.getCreatedAt()
        );
    }
}
