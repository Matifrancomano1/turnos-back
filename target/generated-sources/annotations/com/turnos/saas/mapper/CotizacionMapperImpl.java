package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses;
import com.turnos.saas.model.entity.Cotizacion;
import com.turnos.saas.model.entity.Turno;
import com.turnos.saas.model.enums.CotizacionEstado;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T10:04:05-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CotizacionMapperImpl implements CotizacionMapper {

    @Override
    public Responses.CotizacionResponse toResponse(Cotizacion cotizacion) {
        if ( cotizacion == null ) {
            return null;
        }

        UUID turnoId = null;
        OffsetDateTime creadoEn = null;
        UUID id = null;
        BigDecimal precio = null;
        Integer duracionMinutos = null;
        String descripcion = null;
        CotizacionEstado estado = null;

        turnoId = cotizacionTurnoId( cotizacion );
        creadoEn = cotizacion.getCreatedAt();
        id = cotizacion.getId();
        precio = cotizacion.getPrecio();
        duracionMinutos = cotizacion.getDuracionMinutos();
        descripcion = cotizacion.getDescripcion();
        estado = cotizacion.getEstado();

        Responses.CotizacionResponse cotizacionResponse = new Responses.CotizacionResponse( id, turnoId, precio, duracionMinutos, descripcion, estado, creadoEn );

        return cotizacionResponse;
    }

    @Override
    public Responses.CotizacionInfo toInfo(Cotizacion cotizacion) {
        if ( cotizacion == null ) {
            return null;
        }

        UUID id = null;
        BigDecimal precio = null;
        Integer duracionMinutos = null;
        String descripcion = null;
        CotizacionEstado estado = null;

        id = cotizacion.getId();
        precio = cotizacion.getPrecio();
        duracionMinutos = cotizacion.getDuracionMinutos();
        descripcion = cotizacion.getDescripcion();
        estado = cotizacion.getEstado();

        Responses.CotizacionInfo cotizacionInfo = new Responses.CotizacionInfo( id, precio, duracionMinutos, descripcion, estado );

        return cotizacionInfo;
    }

    private UUID cotizacionTurnoId(Cotizacion cotizacion) {
        if ( cotizacion == null ) {
            return null;
        }
        Turno turno = cotizacion.getTurno();
        if ( turno == null ) {
            return null;
        }
        UUID id = turno.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
