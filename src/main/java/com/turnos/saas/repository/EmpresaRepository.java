package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    Optional<Empresa> findByIdAndActivaTrue(UUID id);
}
