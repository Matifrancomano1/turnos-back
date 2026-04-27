package com.turnos.saas.service;

import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

    private final TurnoRepository turnoRepository;
    private final NotificacionService notificacionService;

    @Scheduled(cron = "0 0 * * * *")
    public void enviarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now();

        // Recordatorio 24hs: turnos entre 23 y 25 horas desde ahora
        turnoRepository.findProgramadosEnRangoHorario(ahora.plusHours(23), ahora.plusHours(25))
                .forEach(turno -> {
                    log.info("Enviando recordatorio 24h para turnoId={}", turno.getId());
                    notificacionService.enviarRecordatorio(turno.getId(), "RECORDATORIO_24H");
                });

        // Recordatorio 2hs: turnos entre 1 y 3 horas desde ahora
        turnoRepository.findProgramadosEnRangoHorario(ahora.plusHours(1), ahora.plusHours(3))
                .forEach(turno -> {
                    log.info("Enviando recordatorio 2h para turnoId={}", turno.getId());
                    notificacionService.enviarRecordatorio(turno.getId(), "RECORDATORIO_2H");
                });
    }
}
