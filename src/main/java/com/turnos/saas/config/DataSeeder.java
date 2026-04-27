package com.turnos.saas.config;

import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Credenciales por defecto ──────────────────────────────────────────────
    private static final String ADMIN_EMAIL    = "admin@turnos.com";
    private static final String ADMIN_NOMBRE   = "Administrador";
    private static final String ADMIN_PASSWORD = "Admin1234!";

    private static final String OPERADOR_EMAIL    = "operador@turnos.com";
    private static final String OPERADOR_NOMBRE   = "Operador";
    private static final String OPERADOR_PASSWORD = "Operador1234!";
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void run(String... args) {
        seedUsuario(ADMIN_EMAIL,    ADMIN_NOMBRE,    ADMIN_PASSWORD,    Rol.ADMIN);
        seedUsuario(OPERADOR_EMAIL, OPERADOR_NOMBRE, OPERADOR_PASSWORD, Rol.OPERADOR);
        printCredenciales();
    }

    private void seedUsuario(String email, String nombre, String rawPassword, Rol rol) {
        if (usuarioRepository.existsByEmail(email)) {
            log.debug("[DataSeeder] Usuario '{}' ya existe, se omite la creación.", email);
            return;
        }
        Usuario u = Usuario.builder()
                .nombre(nombre)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .rol(rol)
                .activo(true)
                .build();
        usuarioRepository.save(u);
        log.info("[DataSeeder] Usuario '{}' creado con rol {}.", email, rol);
    }

    private void printCredenciales() {
        String banner = """

        ╔══════════════════════════════════════════════════════╗
        ║          CREDENCIALES DE ACCESO INICIALES             ║
        ╠══════════════════════════════════════════════════════╣
        ║  ROL       │ EMAIL                  │ CONTRASEÑA      ║
        ╠══════════════════════════════════════════════════════╣
        ║  ADMIN     │ admin@turnos.com       │ Admin1234!      ║
        ║  OPERADOR  │ operador@turnos.com    │ Operador1234!   ║
        ╚══════════════════════════════════════════════════════╝
        """;
        log.info(banner);
    }
}
