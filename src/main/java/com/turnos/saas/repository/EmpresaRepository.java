package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    Optional<Empresa> findByIdAndActivaTrue(UUID id);

    /** Verifica si el slug ya está en uso (creación). */
    boolean existsBySlug(String slug);

    /** Verifica si el slug está en uso por otra empresa distinta (edición). */
    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Empresa> findBySlugAndActivaTrue(String slug);
}
