package com.turnos.saas.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TurnoEstadoTest {

    @Test
    void solicitadoPuedePasarAEnCotizacion() {
        assertThat(TurnoEstado.SOLICITADO.canTransitionTo(TurnoEstado.EN_COTIZACION)).isTrue();
    }

    @Test
    void solicitadoPuedePasarACancelado() {
        assertThat(TurnoEstado.SOLICITADO.canTransitionTo(TurnoEstado.CANCELADO)).isTrue();
    }

    @Test
    void solicitadoNoPuedePasarAFinalizado() {
        assertThat(TurnoEstado.SOLICITADO.canTransitionTo(TurnoEstado.FINALIZADO)).isFalse();
    }

    @Test
    void enCotizacionPuedePasarACotizado() {
        assertThat(TurnoEstado.EN_COTIZACION.canTransitionTo(TurnoEstado.COTIZADO)).isTrue();
    }

    @Test
    void cotizadoPuedePasarAConfirmado() {
        assertThat(TurnoEstado.COTIZADO.canTransitionTo(TurnoEstado.CONFIRMADO)).isTrue();
    }

    @Test
    void confirmadoPuedePasarAProgramado() {
        assertThat(TurnoEstado.CONFIRMADO.canTransitionTo(TurnoEstado.PROGRAMADO)).isTrue();
    }

    @Test
    void programadoPuedePasarAFinalizado() {
        assertThat(TurnoEstado.PROGRAMADO.canTransitionTo(TurnoEstado.FINALIZADO)).isTrue();
    }

    @Test
    void finalizadoNoTieneTransiciones() {
        assertThat(TurnoEstado.FINALIZADO.allowedTransitions()).isEmpty();
    }

    @Test
    void canceladoNoTieneTransiciones() {
        assertThat(TurnoEstado.CANCELADO.allowedTransitions()).isEmpty();
    }

    @Test
    void finalizadoNoPuedeCancelarse() {
        assertThat(TurnoEstado.FINALIZADO.canTransitionTo(TurnoEstado.CANCELADO)).isFalse();
    }

    @Test
    void programadoPuedeCancelarse() {
        assertThat(TurnoEstado.PROGRAMADO.canTransitionTo(TurnoEstado.CANCELADO)).isTrue();
    }

    @Test
    void enCotizacionNoPuedePasarDirectoAConfirmado() {
        assertThat(TurnoEstado.EN_COTIZACION.canTransitionTo(TurnoEstado.CONFIRMADO)).isFalse();
    }
}
