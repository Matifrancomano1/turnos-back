package com.turnos.saas.service;

import com.turnos.saas.dto.request.Requests.*;
import com.turnos.saas.exception.BusinessRuleException;
import com.turnos.saas.exception.InvalidStateTransitionException;
import com.turnos.saas.mapper.CotizacionMapper;
import com.turnos.saas.mapper.TurnoMapper;
import com.turnos.saas.model.entity.*;
import com.turnos.saas.model.enums.Rol;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.*;
import com.turnos.saas.security.JwtUtil;
import com.turnos.saas.security.TenantGuard;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    @Mock TurnoRepository turnoRepository;
    @Mock TurnoHistorialRepository historialRepository;
    @Mock CotizacionRepository cotizacionRepository;
    @Mock SeniaRepository seniaRepository;
    @Mock EmpresaService empresaService;
    @Mock ServicioService servicioService;
    @Mock UsuarioService usuarioService;
    @Mock TurnoMapper turnoMapper;
    @Mock CotizacionMapper cotizacionMapper;
    @Mock JwtUtil jwtUtil;
    @Mock TenantGuard tenantGuard;
    @Mock NotificacionService notificacionService;

    @InjectMocks TurnoService turnoService;

    UUID empresaId;
    UUID turnoId;
    UUID clienteId;
    Turno turno;
    Usuario cliente;
    Claims claims;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        turnoId   = UUID.randomUUID();
        clienteId = UUID.randomUUID();

        cliente = new Usuario();
        cliente.setId(clienteId);
        cliente.setNombre("Test Cliente");
        cliente.setEmail("cliente@test.com");
        cliente.setRol(Rol.CLIENTE);

        Empresa empresa = new Empresa();
        empresa.setId(empresaId);

        turno = new Turno();
        turno.setId(turnoId);
        turno.setCliente(cliente);
        turno.setEmpresa(empresa);
        turno.setEstado(TurnoEstado.CONFIRMADO);
        turno.setFechaConfirmada(LocalDate.now().plusDays(10));
        turno.setHoraConfirmada(LocalTime.of(10, 0));

        claims = mock(Claims.class);
    }

    @Test
    void cancelarComoCliente_conMasDe48Horas_debeFuncionar() {
        turno.setFechaConfirmada(LocalDate.now().plusDays(3));
        turno.setEstado(TurnoEstado.CONFIRMADO);

        when(turnoRepository.findByIdAndEmpresaId(turnoId, empresaId)).thenReturn(Optional.of(turno));
        when(jwtUtil.extractUserId(claims)).thenReturn(clienteId);
        when(jwtUtil.extractRol(claims)).thenReturn(Rol.CLIENTE);
        when(usuarioService.findByIdOrThrow(clienteId)).thenReturn(cliente);
        when(turnoRepository.save(any())).thenReturn(turno);

        assertThatNoException().isThrownBy(() ->
                turnoService.cancelar(empresaId, turnoId, "No puedo asistir", claims));
    }

    @Test
    void cancelarComoCliente_conMenosDe48Horas_debeLanzarBusinessRuleException() {
        turno.setFechaConfirmada(LocalDate.now());
        turno.setHoraConfirmada(LocalTime.now().plusHours(1));
        turno.setEstado(TurnoEstado.CONFIRMADO);

        when(turnoRepository.findByIdAndEmpresaId(turnoId, empresaId)).thenReturn(Optional.of(turno));
        when(jwtUtil.extractUserId(claims)).thenReturn(clienteId);
        when(jwtUtil.extractRol(claims)).thenReturn(Rol.CLIENTE);
        when(usuarioService.findByIdOrThrow(clienteId)).thenReturn(cliente);

        assertThatThrownBy(() -> turnoService.cancelar(empresaId, turnoId, null, claims))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("48 horas");
    }

    @Test
    void cancelarComoOperador_sinRestriccionTemporal_debeFuncionar() {
        UUID operadorId = UUID.randomUUID();
        Usuario operador = new Usuario();
        operador.setId(operadorId);
        operador.setRol(Rol.OPERADOR);

        turno.setFechaConfirmada(LocalDate.now());
        turno.setHoraConfirmada(LocalTime.now().plusHours(1));
        turno.setEstado(TurnoEstado.CONFIRMADO);

        when(turnoRepository.findByIdAndEmpresaId(turnoId, empresaId)).thenReturn(Optional.of(turno));
        when(jwtUtil.extractUserId(claims)).thenReturn(operadorId);
        when(jwtUtil.extractRol(claims)).thenReturn(Rol.OPERADOR);
        when(usuarioService.findByIdOrThrow(operadorId)).thenReturn(operador);
        when(turnoRepository.save(any())).thenReturn(turno);

        assertThatNoException().isThrownBy(() ->
                turnoService.cancelar(empresaId, turnoId, "Cancelado por operador", claims));
    }

    @Test
    void transicionInvalida_debeArrojarInvalidStateTransitionException() {
        turno.setEstado(TurnoEstado.FINALIZADO);

        when(turnoRepository.findByIdAndEmpresaId(turnoId, empresaId)).thenReturn(Optional.of(turno));
        when(jwtUtil.extractUserId(claims)).thenReturn(clienteId);
        when(usuarioService.findByIdOrThrow(clienteId)).thenReturn(cliente);

        CambiarEstadoRequest request = new CambiarEstadoRequest(TurnoEstado.CANCELADO, null);

        assertThatThrownBy(() -> turnoService.cambiarEstado(empresaId, turnoId, request, claims))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void registrarSeniaConSeniaExistente_debeLanzarBusinessRuleException() {
        when(turnoRepository.findByIdAndEmpresaId(turnoId, empresaId)).thenReturn(Optional.of(turno));
        when(seniaRepository.existsByTurnoId(turnoId)).thenReturn(true);

        RegistrarSeniaRequest request = new RegistrarSeniaRequest(
                java.math.BigDecimal.valueOf(1000), "Transferencia", "CVU123");

        assertThatThrownBy(() -> turnoService.registrarSenia(empresaId, turnoId, request, claims))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("seña");
    }
}
