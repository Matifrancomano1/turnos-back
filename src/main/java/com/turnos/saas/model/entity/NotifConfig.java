package com.turnos.saas.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notif_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifConfig {

    @Id
    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "whatsapp_api_token", columnDefinition = "TEXT")
    private String whatsappApiToken;

    @Column(name = "whatsapp_phone_id", length = 50)
    private String whatsappPhoneId;

    @Column(name = "gmail_client_id", length = 200)
    private String gmailClientId;

    @Column(name = "gmail_client_secret", columnDefinition = "TEXT")
    private String gmailClientSecret;

    @Column(name = "whatsapp_enabled", nullable = false)
    @Builder.Default
    private Boolean whatsappEnabled = false;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
