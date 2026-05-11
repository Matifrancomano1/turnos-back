package com.turnos.saas.dto.request;

import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.validation.NoHtml;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class Requests {

    private Requests() {}

    // ==============================
    // AUTH
    // ==============================

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) @NoHtml String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,72}$",
                message = "La contraseña debe contener mayúscula, minúscula, número y carácter especial (8-72 chars)"
            )
            String password,
            @Size(max = 30) String telefono
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}

    public record LogoutRequest(
            @NotBlank String refreshToken
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,72}$",
                message = "La contraseña debe contener mayúscula, minúscula, número y carácter especial (8-72 chars)"
            )
            String newPassword
    ) {}

    // ==============================
    // EMPRESA
    // ==============================

    public record CreateEmpresaRequest(
            @NotBlank @Size(max = 150) @NoHtml String nombre,
            @NotBlank @Email @Size(max = 150) String emailContacto,
            @Size(max = 255) @NoHtml String direccion,
            @Size(max = 30) String telefono,
            /**
             * URL slug único para la empresa. Solo minúsculas, números y guiones.
             * Ejemplo: "mi-negocio-2025"
             */
            @NotBlank
            @Size(min = 3, max = 100, message = "El slug debe tener entre 3 y 100 caracteres")
            @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "El slug solo puede contener letras minúsculas (a-z), números y guiones (-)"
            )
            String slug
    ) {}

    public record UpdateEmpresaRequest(
            @NotBlank @Size(max = 150) @NoHtml String nombre,
            @NotBlank @Email @Size(max = 150) String emailContacto,
            @Size(max = 255) @NoHtml String direccion,
            @Size(max = 30) String telefono,
            @NotBlank
            @Size(min = 3, max = 100, message = "El slug debe tener entre 3 y 100 caracteres")
            @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "El slug solo puede contener letras minúsculas (a-z), números y guiones (-)"
            )
            String slug
    ) {}

    public record UpdateEmpresaConfigRequest(
            @NotNull LocalTime horaApertura,
            @NotNull LocalTime horaCierre,
            /**
             * Duración de cada slot en minutos. Valores soportados: 15, 30, 45, 60.
             * La validación de múltiplo se aplica en la capa de servicio.
             */
            @NotNull @Min(value = 15, message = "La duración mínima de slot es 15 minutos")
            @Max(value = 480, message = "La duración máxima de slot es 480 minutos")
            Integer duracionSlotMinutos,
            @NotNull Boolean sabadoHabilitado,
            @NotNull Boolean domingoHabilitado
    ) {}

    // ==============================
    // USUARIO
    // ==============================

    public record CreateUsuarioRequest(
            @NotBlank @Size(min = 2, max = 100) @NoHtml String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,72}$",
                message = "La contraseña debe contener mayúscula, minúscula, número y carácter especial (8-72 chars)"
            )
            String password,
            @Size(max = 30) String telefono
    ) {}

    public record UpdatePerfilRequest(
            @NotBlank @Size(min = 2, max = 100) @NoHtml String nombre,
            @Size(max = 30) String telefono
    ) {}

    // ==============================
    // SERVICIO
    // ==============================

    public record CreateServicioRequest(
            @NotBlank @Size(max = 150) @NoHtml String nombre,
            @Size(max = 2000) @NoHtml String descripcion,
            @NotNull @DecimalMin("0.0") BigDecimal precioBase,
            @NotNull @Min(1) @Max(1440) Integer duracionEstimadaMinutos
    ) {}

    public record UpdateServicioRequest(
            @NotBlank @Size(max = 150) @NoHtml String nombre,
            @Size(max = 2000) @NoHtml String descripcion,
            @NotNull @DecimalMin("0.0") BigDecimal precioBase,
            @NotNull @Min(1) @Max(1440) Integer duracionEstimadaMinutos
    ) {}

    // ==============================
    // TURNO
    // ==============================

    public record SolicitarTurnoRequest(
            @NotNull UUID servicioId,
            @NotNull LocalDate fechaPreferida,
            @NotNull LocalTime horaPreferida,
            @Size(max = 1000) @NoHtml String observaciones
    ) {}

    public record CambiarEstadoRequest(
            @NotNull TurnoEstado nuevoEstado,
            @Size(max = 500) @NoHtml String motivo
    ) {}

    public record ReprogramarTurnoRequest(
            @NotNull LocalDate nuevaFecha,
            @NotNull LocalTime nuevaHora,
            @Size(max = 500) @NoHtml String motivo
    ) {}

    public record RegistrarSeniaRequest(
            @NotNull @DecimalMin("0.01") BigDecimal monto,
            @NotBlank @Size(max = 50) @NoHtml String metodoPago,
            @Size(max = 200) @NoHtml String referencia
    ) {}

    // ==============================
    // COTIZACION
    // ==============================

    public record CreateCotizacionRequest(
            @NotNull @DecimalMin("0.0") BigDecimal precio,
            @NotNull @Min(1) @Max(1440) Integer duracionMinutos,
            UUID servicioId,
            @Size(max = 2000) @NoHtml String descripcion
    ) {}

    public record UpdateCotizacionRequest(
            @NotNull @DecimalMin("0.0") BigDecimal precio,
            @NotNull @Min(1) @Max(1440) Integer duracionMinutos,
            @Size(max = 2000) @NoHtml String descripcion
    ) {}

    // ==============================
    // AGENDA
    // ==============================

    public record CreateBloqueoRequest(
            @NotNull LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @Size(max = 500) @NoHtml String motivo
    ) {}

    // ==============================
    // NOTIFICACIONES
    // ==============================

    public record UpdateNotifConfigRequest(
            String whatsappApiToken,
            String whatsappPhoneNumberId,
            String gmailClientId,
            String gmailClientSecret,
            @NotNull Boolean whatsappEnabled,
            @NotNull Boolean emailEnabled
    ) {}

    // ==============================
    // SUPER ADMIN
    // ==============================

    public record CreateTenantAdminRequest(
            // Empresa
            @NotBlank @Size(max = 150) @NoHtml String nombre,
            @NotBlank @Email @Size(max = 150) String emailContacto,
            @Size(max = 255) @NoHtml String direccion,
            @Size(max = 30) String telefono,
            @NotBlank
            @Size(min = 3, max = 100, message = "El slug debe tener entre 3 y 100 caracteres")
            @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "El slug solo puede contener letras minúsculas (a-z), números y guiones (-)"
            )
            String slug,
            // Usuario admin inicial
            @NotBlank @Size(min = 2, max = 100) @NoHtml String adminNombre,
            @NotBlank @Email @Size(max = 150) String adminEmail,
            @NotBlank
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,72}$",
                message = "La contraseña debe contener mayúscula, minúscula, número y carácter especial (8-72 chars)"
            )
            String adminPassword,
            @Size(max = 30) String adminTelefono
    ) {}
}
