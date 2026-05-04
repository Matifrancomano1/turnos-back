package com.turnos.saas.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "email_contacto", nullable = false, length = 150)
    private String emailContacto;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    /**
     * Identificador único legible para la URL pública de la empresa.
     * Ejemplo: "mi-taller-mecanico" → /turnos/mi-taller-mecanico
     * Solo admite letras minúsculas, números y guiones: ^[a-z0-9-]+$
     */
    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    @Column(name = "duracion_slot_minutos", nullable = false)
    @Builder.Default
    private Integer duracionSlotMinutos = 30;

    @Column(name = "sabado_habilitado", nullable = false)
    @Builder.Default
    private Boolean sabadoHabilitado = false;

    @Column(name = "domingo_habilitado", nullable = false)
    @Builder.Default
    private Boolean domingoHabilitado = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
