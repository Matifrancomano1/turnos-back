package com.turnos.saas.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Configuración global de la plataforma SaaS.
 * Tabla singleton: siempre existe exactamente UN registro con id = 1.
 * El SuperAdmin la gestiona desde /api/v1/superadmin/config.
 */
@Entity
@Table(name = "saas_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasConfig {

    /** Clave primaria fija – siempre 1. */
    @Id
    @Column(nullable = false)
    private Integer id;

    // ── Identidad de la plataforma ──────────────────────────────────────
    @Column(name = "platform_name", nullable = false, length = 150)
    @Builder.Default
    private String platformName = "SaaS Turnos";

    @Column(name = "base_domain", length = 255)
    @Builder.Default
    private String baseDomain = "turnos.app";

    @Column(name = "support_email", length = 150)
    @Builder.Default
    private String supportEmail = "soporte@turnos.app";

    // ── Límites por tenant ───────────────────────────────────────────────
    @Column(name = "default_max_users", nullable = false)
    @Builder.Default
    private Integer defaultMaxUsers = 10;

    @Column(name = "default_max_turnos_mensuales", nullable = false)
    @Builder.Default
    private Integer defaultMaxTurnosMensuales = 200;

    // ── Plan de prueba ───────────────────────────────────────────────────
    @Column(name = "allow_trial", nullable = false)
    @Builder.Default
    private Boolean allowTrial = true;

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private Integer trialDays = 14;

    // ── SMTP saliente ────────────────────────────────────────────────────
    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    @Builder.Default
    private Integer smtpPort = 587;

    @Column(name = "smtp_user", length = 150)
    private String smtpUser;

    /** Contraseña/app-password del SMTP. Se guarda en texto plano (o cifrado a nivel infra). */
    @Column(name = "smtp_pass", length = 255)
    private String smtpPass;

    @Column(name = "smtp_from", length = 150)
    private String smtpFrom;

    // ── Auditoría ────────────────────────────────────────────────────────
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
