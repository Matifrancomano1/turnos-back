package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);

    Optional<Notificacion> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.id = :id AND n.usuario.id = :usuarioId")
    int marcarLeida(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);
}
