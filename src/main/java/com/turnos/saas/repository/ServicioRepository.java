package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    Page<Servicio> findByEmpresaId(UUID empresaId, Pageable pageable);

    Page<Servicio> findByEmpresaIdAndActivoTrue(UUID empresaId, Pageable pageable);

    Optional<Servicio> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
