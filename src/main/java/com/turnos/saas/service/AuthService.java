package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.ResourceNotFoundException;
import com.turnos.saas.mapper.UsuarioMapper;
import com.turnos.saas.model.entity.RefreshToken;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.repository.UsuarioRepository;
import com.turnos.saas.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioMapper usuarioMapper;
    private final NotificacionService notificacionService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(Rol.CLIENTE)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario registrado: id={}, email={}", usuario.getId(), usuario.getEmail());

        return buildAuthResponse(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadCredentialsException("Tu cuenta se encuentra deshabilitada");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        log.info("Login exitoso: userId={}", usuario.getId());
        return buildAuthResponse(usuario);
    }

    @Transactional
    public RefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenService.validar(request.refreshToken());
        Usuario usuario = storedToken.getUsuario();

        refreshTokenService.revocar(request.refreshToken());

        String newAccessToken  = jwtUtil.generateAccessToken(
                usuario.getId(),
                usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
                usuario.getRol()
        );
        String newRefreshToken = jwtUtil.generateRefreshToken(usuario.getId());
        refreshTokenService.crear(usuario, newRefreshToken);

        return new RefreshResponse(newAccessToken, newRefreshToken, jwtUtil.getAccessExpirationMs() / 1000);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revocar(request.refreshToken());
        log.info("Logout: refreshToken revoked");
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            String rawToken = UUID.randomUUID().toString();
            refreshTokenService.crearPasswordResetToken(usuario, rawToken);
            notificacionService.enviarEmailPasswordReset(usuario, rawToken);
            log.info("Password reset token generado para userId={}", usuario.getId());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        RefreshToken resetToken = refreshTokenService.validarPasswordResetToken(request.token());
        Usuario usuario = resetToken.getUsuario();

        usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        usuarioRepository.save(usuario);

        resetToken.setRevocado(true);
        refreshTokenService.revocarTodosDelUsuario(usuario.getId());

        log.info("Password restablecida para userId={}", usuario.getId());
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        UUID empresaId = usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null;
        String accessToken  = jwtUtil.generateAccessToken(usuario.getId(), empresaId, usuario.getRol());
        String refreshToken = jwtUtil.generateRefreshToken(usuario.getId());
        refreshTokenService.crear(usuario, refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getAccessExpirationMs() / 1000,
                usuarioMapper.toInfo(usuario)
        );
    }
}
