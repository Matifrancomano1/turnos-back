package com.turnos.saas.repository;

import com.turnos.saas.model.entity.TurnoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TurnoHistorialRepository extends JpaRepository<TurnoHistorial, UUID> {

    List<TurnoHistorial> findByTurnoIdOrderByTimestampAsc(UUID turnoId);
}
