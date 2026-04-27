package com.turnos.saas.repository;

import com.turnos.saas.model.entity.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, UUID> {

    List<BloqueoAgenda> findByEmpresaIdAndFecha(UUID empresaId, LocalDate fecha);

    @Query("""
        SELECT b FROM BloqueoAgenda b
        WHERE b.empresa.id = :empresaId
          AND b.fecha BETWEEN :desde AND :hasta
        ORDER BY b.fecha, b.horaInicio
        """)
    List<BloqueoAgenda> findByEmpresaIdAndFechaBetween(
            @Param("empresaId") UUID empresaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );
}
