package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID> {

    Optional<Cotizacion> findByTurnoId(UUID turnoId);
}
