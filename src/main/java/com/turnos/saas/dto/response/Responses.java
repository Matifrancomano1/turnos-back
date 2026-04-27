package com.turnos.saas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.turnos.saas.model.enums.CotizacionEstado;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.model.enums.TurnoEstado;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

public final class Responses {

    private Responses() {}

    // ==============================
    // ENVELOPE
    // ==============================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiResponse<T>(
            String status,
            String message,
            T data,
            Instant timestamp
    ) {
        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>("OK", "Operación exitosa", data, Instant.now());
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>("OK", message, data, Instant.now());
        }

        public static <T> ApiResponse<T> created(String message, T data) {
            return new ApiResponse<>("CREATED", message, data, Instant.now());
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {}

    // ==============================
    // AUTH
    // ==============================

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UsuarioInfo usuario
    ) {}

    public record RefreshResponse(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {}

    public record UsuarioInfo(
            UUID id,
            String nombre,
            String email,
            Rol rol,
            UUID empresaId
    ) {}

    // ==============================
    // EMPRESA
    // ==============================

    public record EmpresaResponse(
            UUID id,
            String nombre,
            String emailContacto,
            String direccion,
            String telefono,
            EmpresaConfigResponse config,
            Boolean activa,
            OffsetDateTime creadoEn
    ) {}

    public record EmpresaConfigResponse(
            LocalTime horaApertura,
            LocalTime horaCierre,
            Integer duracionSlotMinutos,
            Boolean sabadoHabilitado,
            Boolean domingoHabilitado
    ) {}

    // ==============================
    // USUARIO
    // ==============================

    public record UsuarioResponse(
            UUID id,
            String nombre,
            String email,
            String telefono,
            Rol rol,
            UUID empresaId,
            Boolean activo,
            OffsetDateTime creadoEn
    ) {}

    // ==============================
    // SERVICIO
    // ==============================

    public record ServicioResponse(
            UUID id,
            UUID empresaId,
            String nombre,
            String descripcion,
            BigDecimal precioBase,
            Integer duracionEstimadaMinutos,
            Boolean activo,
            OffsetDateTime creadoEn
    ) {}

    // ==============================
    // TURNO
    // ==============================

    public record TurnoResponse(
            UUID id,
            UUID empresaId,
            ClienteInfo cliente,
            ServicioInfo servicio,
            LocalDate fechaSolicitada,
            LocalTime horaSolicitada,
            LocalDate fechaConfirmada,
            LocalTime horaConfirmada,
            TurnoEstado estado,
            String observaciones,
            CotizacionInfo cotizacion,
            SeniaInfo senia,
            List<TurnoHistorialEntry> historial,
            OffsetDateTime creadoEn
    ) {}

    public record TurnoSummary(
            UUID id,
            LocalDate fecha,
            LocalTime hora,
            TurnoEstado estado,
            String nombreCliente,
            String nombreServicio
    ) {}

    public record TurnoHistorialEntry(
            TurnoEstado estadoAnterior,
            TurnoEstado estadoNuevo,
            String motivo,
            String cambiadoPor,
            OffsetDateTime timestamp
    ) {}

    public record CotizacionInfo(
            UUID id,
            BigDecimal precio,
            Integer duracionMinutos,
            String descripcion,
            CotizacionEstado estado
    ) {}

    public record SeniaInfo(
            UUID id,
            BigDecimal monto,
            String metodoPago,
            String referencia,
            OffsetDateTime registradoEn
    ) {}

    public record ClienteInfo(
            UUID id,
            String nombre,
            String email
    ) {}

    public record ServicioInfo(
            UUID id,
            String nombre
    ) {}

    // ==============================
    // COTIZACION
    // ==============================

    public record CotizacionResponse(
            UUID id,
            UUID turnoId,
            BigDecimal precio,
            Integer duracionMinutos,
            String descripcion,
            CotizacionEstado estado,
            OffsetDateTime creadoEn
    ) {}

    // ==============================
    // AGENDA
    // ==============================

    public record SlotDisponibilidad(
            LocalDate fecha,
            LocalTime hora,
            boolean disponible
    ) {}

    public record DisponibilidadResponse(
            LocalDate fecha,
            List<SlotDisponibilidad> slots
    ) {}

    public record BloqueoResponse(
            UUID id,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo,
            String creadoPor,
            OffsetDateTime creadoEn
    ) {}

    public record CalendarioResponse(
            LocalDate fecha,
            List<TurnoSummary> turnos,
            List<BloqueoResponse> bloqueos
    ) {}

    // ==============================
    // NOTIFICACIONES
    // ==============================

    public record NotificacionResponse(
            UUID id,
            String tipo,
            String titulo,
            String cuerpo,
            Boolean leida,
            OffsetDateTime creadoEn
    ) {}

    public record NotifConfigResponse(
            Boolean whatsappEnabled,
            Boolean emailEnabled,
            Boolean whatsappConfigured,
            Boolean gmailConfigured
    ) {}

    // ==============================
    // REPORTES
    // ==============================

    public record DashboardResponse(
            long turnosHoy,
            long turnosMes,
            long turnosCancelados,
            double tasaCancelacion,
            BigDecimal ingresosEstimados,
            List<TopServicioItem> topServicios
    ) {}

    public record TopServicioItem(
            String nombre,
            long cantidad,
            BigDecimal ingresoTotal
    ) {}

    // ==============================
    // ERROR
    // ==============================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(
            String error,
            String message,
            Object fields,
            Instant timestamp
    ) {
        public static ErrorResponse of(String error, String message) {
            return new ErrorResponse(error, message, null, Instant.now());
        }

        public static ErrorResponse of(String error, String message, Object fields) {
            return new ErrorResponse(error, message, fields, Instant.now());
        }
    }
}
