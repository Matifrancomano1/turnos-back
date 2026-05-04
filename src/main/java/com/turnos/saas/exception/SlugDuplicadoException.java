package com.turnos.saas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se intenta crear o actualizar una empresa
 * con un slug que ya existe en la base de datos.
 * Resulta en una respuesta HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SlugDuplicadoException extends RuntimeException {

    public SlugDuplicadoException(String slug) {
        super("El slug '" + slug + "' ya está en uso por otra empresa.");
    }
}
