package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Senia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeniaRepository extends JpaRepository<Senia, UUID> {

    Optional<Senia> findByTurnoId(UUID turnoId);

    boolean existsByTurnoId(UUID turnoId);
}
