package com.turnos.saas.repository;

import com.turnos.saas.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiraEn < :now")
    void deleteAllExpired(OffsetDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.usuario.id = :usuarioId AND rt.revocado = false")
    void revokeAllByUsuarioId(UUID usuarioId);
}
