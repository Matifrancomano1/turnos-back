package com.turnos.saas.repository;

import com.turnos.saas.model.entity.NotifConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotifConfigRepository extends JpaRepository<NotifConfig, UUID> {

    Optional<NotifConfig> findByEmpresaId(UUID empresaId);
}
