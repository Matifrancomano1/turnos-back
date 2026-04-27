package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses.EmpresaConfigResponse;
import com.turnos.saas.dto.response.Responses.EmpresaResponse;
import com.turnos.saas.model.entity.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring",
        imports = {com.turnos.saas.dto.response.Responses.EmpresaConfigResponse.class})
public interface EmpresaMapper {

    @Mapping(target = "config", expression = "java(new EmpresaConfigResponse(empresa.getHoraApertura(), empresa.getHoraCierre(), empresa.getDuracionSlotMinutos(), empresa.getSabadoHabilitado(), empresa.getDomingoHabilitado()))")
    @Mapping(target = "creadoEn", source = "createdAt")
    EmpresaResponse toResponse(Empresa empresa);

    List<EmpresaResponse> toResponseList(List<Empresa> empresas);
}
