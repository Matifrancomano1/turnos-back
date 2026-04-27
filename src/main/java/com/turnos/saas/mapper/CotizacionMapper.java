package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.entity.Cotizacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CotizacionMapper {

    @Mapping(target = "turnoId",  source = "turno.id")
    @Mapping(target = "creadoEn", source = "createdAt")
    CotizacionResponse toResponse(Cotizacion cotizacion);

    @Mapping(target = "id",              source = "id")
    @Mapping(target = "precio",          source = "precio")
    @Mapping(target = "duracionMinutos", source = "duracionMinutos")
    @Mapping(target = "descripcion",     source = "descripcion")
    @Mapping(target = "estado",          source = "estado")
    CotizacionInfo toInfo(Cotizacion cotizacion);
}
