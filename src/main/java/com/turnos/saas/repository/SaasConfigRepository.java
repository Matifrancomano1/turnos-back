package com.turnos.saas.repository;

import com.turnos.saas.model.entity.SaasConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la configuración global de la plataforma.
 * Solo existe UN registro (id = 1), por lo que basta con findById(1).
 */
@Repository
public interface SaasConfigRepository extends JpaRepository<SaasConfig, Integer> {
}
