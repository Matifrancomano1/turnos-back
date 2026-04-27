package com.turnos.saas.service;

import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.model.entity.RefreshToken;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken crear(Usuario usuario, String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash)
                .tipo("REFRESH")
                .expiraEn(OffsetDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken validar(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("Refresh token inválido"));

        if (Boolean.TRUE.equals(token.getRevocado())) {
            throw new BusinessRuleException("Refresh token revocado");
        }
        if (token.getExpiraEn().isBefore(OffsetDateTime.now())) {
            throw new BusinessRuleException("Refresh token expirado");
        }
        return token;
    }

    @Transactional
    public void revocar(String rawToken) {
        String hash = sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevocado(true);
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public void revocarTodosDelUsuario(java.util.UUID usuarioId) {
        refreshTokenRepository.revokeAllByUsuarioId(usuarioId);
    }

    @Transactional
    public void crearPasswordResetToken(Usuario usuario, String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash)
                .tipo("PASSWORD_RESET")
                .expiraEn(OffsetDateTime.now().plusHours(1))
                .build();
        refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken validarPasswordResetToken(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("Token de recuperación inválido"));

        if (!"PASSWORD_RESET".equals(token.getTipo())) {
            throw new BusinessRuleException("Token de recuperación inválido");
        }
        if (Boolean.TRUE.equals(token.getRevocado())) {
            throw new BusinessRuleException("Token de recuperación ya utilizado");
        }
        if (token.getExpiraEn().isBefore(OffsetDateTime.now())) {
            throw new BusinessRuleException("Token de recuperación expirado");
        }
        return token;
    }

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
