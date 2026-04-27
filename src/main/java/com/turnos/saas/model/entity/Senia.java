package com.turnos.saas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "senias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Senia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @Column(length = 200)
    private String referencia;

    @Column(name = "registrado_en", nullable = false)
    @Builder.Default
    private OffsetDateTime registradoEn = OffsetDateTime.now();
}
