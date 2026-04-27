package com.turnos.saas.service;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.model.entity.*;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotifConfigRepository notifConfigRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;

    @Async
    public void notificarTransicion(UUID turnoId, TurnoEstado nuevoEstado, UUID empresaId) {
        try {
            Turno turno = turnoRepository.findById(turnoId).orElse(null);
            if (turno == null) return;

            NotifConfig config = notifConfigRepository.findByEmpresaId(empresaId).orElse(null);
            if (config == null) return;

            String titulo = buildTitulo(nuevoEstado);
            String cuerpo  = buildCuerpo(nuevoEstado, turno);

            guardarNotificacion(turno.getCliente(), "TURNO_" + nuevoEstado.name(), titulo, cuerpo);

            if (Boolean.TRUE.equals(config.getEmailEnabled())) {
                enviarEmail(turno.getCliente().getEmail(), titulo, cuerpo);
            }
            if (Boolean.TRUE.equals(config.getWhatsappEnabled()) && turno.getCliente().getTelefono() != null) {
                enviarWhatsApp(config, turno.getCliente().getTelefono(), nuevoEstado.name(), turno);
            }

            log.info("Notificación enviada: turnoId={}, estado={}", turnoId, nuevoEstado);
        } catch (Exception e) {
            log.error("Error enviando notificación para turnoId={}: {}", turnoId, e.getMessage());
        }
    }

    @Async
    public void notificarReprogramacion(UUID turnoId, UUID empresaId) {
        try {
            Turno turno = turnoRepository.findById(turnoId).orElse(null);
            if (turno == null) return;

            NotifConfig config = notifConfigRepository.findByEmpresaId(empresaId).orElse(null);
            String titulo = "Turno reprogramado";
            String cuerpo = "Su turno ha sido reprogramado para el " +
                    turno.getFechaConfirmada() + " a las " + turno.getHoraConfirmada();

            guardarNotificacion(turno.getCliente(), "REPROGRAMACION", titulo, cuerpo);
            if (config != null && Boolean.TRUE.equals(config.getEmailEnabled())) {
                enviarEmail(turno.getCliente().getEmail(), titulo, cuerpo);
            }
        } catch (Exception e) {
            log.error("Error notificando reprogramación turnoId={}: {}", turnoId, e.getMessage());
        }
    }

    @Async
    public void enviarEmailPasswordReset(Usuario usuario, String rawToken) {
        try {
            String asunto = "Recuperación de contraseña — Turnos SaaS";
            String cuerpo = "<p>Haga clic en el siguiente enlace para restablecer su contraseña:</p>" +
                    "<p><a href='http://localhost:8080/reset-password?token=" + rawToken + "'>Restablecer contraseña</a></p>" +
                    "<p>Este enlace expira en 1 hora.</p>";
            enviarEmail(usuario.getEmail(), asunto, cuerpo);
        } catch (Exception e) {
            log.error("Error enviando email de reset password a {}: {}", usuario.getEmail(), e.getMessage());
        }
    }

    @Async
    public void enviarRecordatorio(UUID turnoId, String tipo) {
        try {
            Turno turno = turnoRepository.findById(turnoId).orElse(null);
            if (turno == null) return;

            NotifConfig config = notifConfigRepository.findByEmpresaId(turno.getEmpresa().getId()).orElse(null);
            String horas = "RECORDATORIO_24H".equals(tipo) ? "24 horas" : "2 horas";
            String titulo = "Recordatorio de turno";
            String cuerpo = "Le recordamos que tiene un turno programado en aproximadamente " + horas + ". " +
                    "Fecha: " + turno.getFechaConfirmada() + " a las " + turno.getHoraConfirmada() + ".";

            guardarNotificacion(turno.getCliente(), tipo, titulo, cuerpo);
            if (config != null && Boolean.TRUE.equals(config.getEmailEnabled())) {
                enviarEmail(turno.getCliente().getEmail(), titulo, cuerpo);
            }
            if (config != null && Boolean.TRUE.equals(config.getWhatsappEnabled()) &&
                    turno.getCliente().getTelefono() != null) {
                enviarWhatsApp(config, turno.getCliente().getTelefono(), tipo, turno);
            }
        } catch (Exception e) {
            log.error("Error enviando recordatorio para turnoId={}: {}", turnoId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificacionResponse> listarMias(Claims claims, Pageable pageable) {
        UUID userId = UUID.fromString(claims.getSubject());
        return notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(userId, pageable)
                .map(n -> new NotificacionResponse(n.getId(), n.getTipo(), n.getTitulo(),
                        n.getCuerpo(), n.getLeida(), n.getCreatedAt()));
    }

    @Transactional
    public void marcarLeida(UUID notifId, Claims claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        int updated = notificacionRepository.marcarLeida(notifId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notificación", notifId);
        }
    }

    @Transactional(readOnly = true)
    public NotifConfigResponse getConfig(UUID empresaId) {
        NotifConfig config = notifConfigRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("NotifConfig", empresaId));
        return new NotifConfigResponse(
                config.getWhatsappEnabled(),
                config.getEmailEnabled(),
                config.getWhatsappApiToken() != null && !config.getWhatsappApiToken().isBlank(),
                config.getGmailClientId() != null && !config.getGmailClientId().isBlank()
        );
    }

    @Transactional
    public NotifConfigResponse actualizarConfig(UUID empresaId,
            com.turnos.saas.dto.request.Requests.UpdateNotifConfigRequest request) {
        NotifConfig config = notifConfigRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("NotifConfig", empresaId));

        if (request.whatsappApiToken() != null) config.setWhatsappApiToken(request.whatsappApiToken());
        if (request.whatsappPhoneNumberId() != null) config.setWhatsappPhoneId(request.whatsappPhoneNumberId());
        if (request.gmailClientId() != null) config.setGmailClientId(request.gmailClientId());
        if (request.gmailClientSecret() != null) config.setGmailClientSecret(request.gmailClientSecret());
        config.setWhatsappEnabled(request.whatsappEnabled());
        config.setEmailEnabled(request.emailEnabled());

        config = notifConfigRepository.save(config);
        log.info("NotifConfig actualizada: empresaId={}", empresaId);
        return new NotifConfigResponse(
                config.getWhatsappEnabled(), config.getEmailEnabled(),
                config.getWhatsappApiToken() != null && !config.getWhatsappApiToken().isBlank(),
                config.getGmailClientId() != null && !config.getGmailClientId().isBlank()
        );
    }

    @Async
    public void enviarTest(UUID empresaId, String canal) {
        try {
            NotifConfig config = notifConfigRepository.findByEmpresaId(empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("NotifConfig", empresaId));
            if ("EMAIL".equalsIgnoreCase(canal)) {
                enviarEmail(config.getGmailClientId() != null ? config.getGmailClientId() : "test@test.com",
                        "Test notificación", "<p>Email de prueba del sistema de turnos.</p>");
            } else if ("WHATSAPP".equalsIgnoreCase(canal)) {
                log.info("WhatsApp test enviado para empresaId={}", empresaId);
            }
        } catch (Exception e) {
            log.error("Error en test de notificación: {}", e.getMessage());
        }
    }

    private void guardarNotificacion(Usuario usuario, String tipo, String titulo, String cuerpo) {
        Notificacion notif = Notificacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .titulo(titulo)
                .cuerpo(cuerpo)
                .build();
        notificacionRepository.save(notif);
    }

    private void enviarEmail(String destinatario, String asunto, String cuerpoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
        }
    }

    private void enviarWhatsApp(NotifConfig config, String telefono, String tipo, Turno turno) {
        try {
            if (config.getWhatsappApiToken() == null || config.getWhatsappPhoneId() == null) return;

            String body = """
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "template",
                      "template": {
                        "name": "turno_notificacion",
                        "language": { "code": "es_AR" },
                        "components": [{"type": "body", "parameters": [{"type": "text", "text": "%s"}]}]
                      }
                    }
                    """.formatted(telefono, tipo);

            RestClient.create()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/" + config.getWhatsappPhoneId() + "/messages")
                    .header("Authorization", "Bearer " + config.getWhatsappApiToken())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp enviado: tipo={}, telefono={}", tipo, "****");
        } catch (Exception e) {
            log.error("Error enviando WhatsApp: {}", e.getMessage());
        }
    }

    private String buildTitulo(TurnoEstado estado) {
        return switch (estado) {
            case COTIZADO   -> "Nueva cotización disponible";
            case CONFIRMADO -> "Turno confirmado";
            case FINALIZADO -> "Turno finalizado";
            case CANCELADO  -> "Turno cancelado";
            default         -> "Actualización de tu turno";
        };
    }

    private String buildCuerpo(TurnoEstado estado, Turno turno) {
        return switch (estado) {
            case COTIZADO   -> "Se ha generado una cotización para tu solicitud. Por favor, revisala y respondé.";
            case CONFIRMADO -> "Tu turno fue confirmado para el " + turno.getFechaConfirmada()
                    + " a las " + turno.getHoraConfirmada() + ".";
            case FINALIZADO -> "Tu turno del " + turno.getFechaConfirmada() + " fue completado. ¡Gracias!";
            case CANCELADO  -> "Tu turno ha sido cancelado.";
            default         -> "El estado de tu turno fue actualizado a: " + estado.name();
        };
    }
}
