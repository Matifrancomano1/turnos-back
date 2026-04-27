package com.turnos.saas.model.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TurnoEstado {

    SOLICITADO,
    EN_COTIZACION,
    COTIZADO,
    CONFIRMADO,
    PROGRAMADO,
    FINALIZADO,
    CANCELADO;

    private static final Map<TurnoEstado, Set<TurnoEstado>> VALID_TRANSITIONS = Map.of(
            SOLICITADO,    EnumSet.of(EN_COTIZACION, CANCELADO),
            EN_COTIZACION, EnumSet.of(COTIZADO, CANCELADO),
            COTIZADO,      EnumSet.of(CONFIRMADO, CANCELADO),
            CONFIRMADO,    EnumSet.of(PROGRAMADO, CANCELADO),
            PROGRAMADO,    EnumSet.of(FINALIZADO, CANCELADO),
            FINALIZADO,    EnumSet.noneOf(TurnoEstado.class),
            CANCELADO,     EnumSet.noneOf(TurnoEstado.class)
    );

    public boolean canTransitionTo(TurnoEstado next) {
        Set<TurnoEstado> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(next);
    }

    public Set<TurnoEstado> allowedTransitions() {
        return VALID_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(TurnoEstado.class));
    }
}
