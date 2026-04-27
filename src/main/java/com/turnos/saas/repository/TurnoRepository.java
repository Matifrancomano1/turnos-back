package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Turno;
import com.turnos.saas.model.enums.TurnoEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, UUID> {

    Optional<Turno> findByIdAndEmpresaId(UUID id, UUID empresaId);

    @Query("""
        SELECT t FROM Turno t
        WHERE t.empresa.id = :empresaId
          AND (:estado IS NULL OR t.estado = :estado)
          AND (:fecha IS NULL OR t.fechaConfirmada = :fecha)
          AND (:clienteId IS NULL OR t.cliente.id = :clienteId)
        """)
    Page<Turno> findByFilters(
            @Param("empresaId") UUID empresaId,
            @Param("estado") TurnoEstado estado,
            @Param("fecha") LocalDate fecha,
            @Param("clienteId") UUID clienteId,
            Pageable pageable
    );

    Page<Turno> findByClienteId(UUID clienteId, Pageable pageable);

    @Query("""
        SELECT t FROM Turno t
        WHERE t.empresa.id = :empresaId
          AND t.estado IN :estados
          AND t.fechaConfirmada = :fecha
        """)
    List<Turno> findByEmpresaIdAndEstadoInAndFechaConfirmada(
            @Param("empresaId") UUID empresaId,
            @Param("estados") List<TurnoEstado> estados,
            @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT t FROM Turno t
        WHERE t.estado = 'PROGRAMADO'
          AND FUNCTION('timestamp', t.fechaConfirmada, t.horaConfirmada)
              BETWEEN :desde AND :hasta
        """)
    List<Turno> findProgramadosEnRangoHorario(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT t FROM Turno t
        WHERE t.empresa.id = :empresaId
          AND t.estado IN :estados
          AND t.fechaConfirmada BETWEEN :desde AND :hasta
        """)
    List<Turno> findByEmpresaIdAndEstadoInAndFechaRange(
            @Param("empresaId") UUID empresaId,
            @Param("estados") List<TurnoEstado> estados,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
        SELECT COUNT(t) FROM Turno t
        WHERE t.empresa.id = :empresaId
          AND t.fechaConfirmada = :fecha
        """)
    long countByEmpresaIdAndFecha(@Param("empresaId") UUID empresaId, @Param("fecha") LocalDate fecha);
}
