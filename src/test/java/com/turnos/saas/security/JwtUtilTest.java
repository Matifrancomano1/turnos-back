package com.turnos.saas.security;

import com.turnos.saas.model.enums.Rol;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "test-secret-de-al-menos-32-caracteres-seguro";
    private static final long ACCESS_MS  = 900_000L;
    private static final long REFRESH_MS = 604_800_000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_MS, REFRESH_MS);
    }

    @Test
    void generarYParsearAccessToken_debeExtraerClaimsCorrectos() {
        UUID userId    = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        Rol rol        = Rol.OPERADOR;

        String token = jwtUtil.generateAccessToken(userId, empresaId, rol);
        Claims claims = jwtUtil.parseToken(token);

        assertThat(jwtUtil.isAccessToken(claims)).isTrue();
        assertThat(jwtUtil.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtUtil.extractEmpresaId(claims)).isEqualTo(empresaId);
        assertThat(jwtUtil.extractRol(claims)).isEqualTo(rol);
    }

    @Test
    void generarRefreshToken_debeSerTipoRefresh() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateRefreshToken(userId);
        Claims claims = jwtUtil.parseToken(token);

        assertThat(jwtUtil.isRefreshToken(claims)).isTrue();
        assertThat(jwtUtil.isAccessToken(claims)).isFalse();
        assertThat(jwtUtil.extractUserId(claims)).isEqualTo(userId);
    }

    @Test
    void accessTokenConEmpresaIdNulo_debeRetornarNullEnExtraccion() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, null, Rol.CLIENTE);
        Claims claims = jwtUtil.parseToken(token);

        assertThat(jwtUtil.extractEmpresaId(claims)).isNull();
    }

    @Test
    void tokenConSecretoInvalido_debeLanzarExcepcion() {
        JwtUtil otroUtil = new JwtUtil("otro-secreto-diferente-de-al-menos-32-chars!", ACCESS_MS, REFRESH_MS);
        UUID userId = UUID.randomUUID();
        String token = otroUtil.generateAccessToken(userId, null, Rol.CLIENTE);

        assertThatThrownBy(() -> jwtUtil.parseToken(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void accessExpirationMsDebeRetornarValorConfigurable() {
        assertThat(jwtUtil.getAccessExpirationMs()).isEqualTo(ACCESS_MS);
    }
}
