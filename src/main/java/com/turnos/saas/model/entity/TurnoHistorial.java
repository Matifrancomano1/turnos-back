package com.turnos.saas.model.entity;

import com.turnos.saas.model.enums.TurnoEstado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "turno_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_anterior", columnDefinition = "turno_estado")
    private TurnoEstado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_nuevo", nullable = false, columnDefinition = "turno_estado")
    private TurnoEstado estadoNuevo;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cambiado_por_id")
    private Usuario cambiadoPor;

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();
}
