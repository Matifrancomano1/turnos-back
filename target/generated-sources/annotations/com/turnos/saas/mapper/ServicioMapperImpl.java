package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.Servicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-04T11:22:55-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ServicioMapperImpl implements ServicioMapper {

    @Override
    public Responses.ServicioResponse toResponse(Servicio servicio) {
        if ( servicio == null ) {
            return null;
        }

        UUID empresaId = null;
        OffsetDateTime creadoEn = null;
        UUID id = null;
        String nombre = null;
        String descripcion = null;
        BigDecimal precioBase = null;
        Integer duracionEstimadaMinutos = null;
        Boolean activo = null;

        empresaId = servicioEmpresaId( servicio );
        creadoEn = servicio.getCreatedAt();
        id = servicio.getId();
        nombre = servicio.getNombre();
        descripcion = servicio.getDescripcion();
        precioBase = servicio.getPrecioBase();
        duracionEstimadaMinutos = servicio.getDuracionEstimadaMinutos();
        activo = servicio.getActivo();

        Responses.ServicioResponse servicioResponse = new Responses.ServicioResponse( id, empresaId, nombre, descripcion, precioBase, duracionEstimadaMinutos, activo, creadoEn );

        return servicioResponse;
    }

    @Override
    public List<Responses.ServicioResponse> toResponseList(List<Servicio> servicios) {
        if ( servicios == null ) {
            return null;
        }

        List<Responses.ServicioResponse> list = new ArrayList<Responses.ServicioResponse>( servicios.size() );
        for ( Servicio servicio : servicios ) {
            list.add( toResponse( servicio ) );
        }

        return list;
    }

    private UUID servicioEmpresaId(Servicio servicio) {
        if ( servicio == null ) {
            return null;
        }
        Empresa empresa = servicio.getEmpresa();
        if ( empresa == null ) {
            return null;
        }
        UUID id = empresa.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
