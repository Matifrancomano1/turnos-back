package com.turnos.saas.exception;

import com.turnos.saas.model.enums.TurnoEstado;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(TurnoEstado from, TurnoEstado to) {
        super("Cannot transition turno from " + from + " to " + to);
    }
}
