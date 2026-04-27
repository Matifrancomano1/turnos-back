package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.entity.Servicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServicioMapper {

    @Mapping(target = "empresaId", source = "empresa.id")
    @Mapping(target = "creadoEn",  source = "createdAt")
    ServicioResponse toResponse(Servicio servicio);

    List<ServicioResponse> toResponseList(List<Servicio> servicios);
}
