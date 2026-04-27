package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "empresaId", expression = "java(usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null)")
    @Mapping(target = "creadoEn", source = "createdAt")
    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "empresaId", expression = "java(usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null)")
    UsuarioInfo toInfo(Usuario usuario);

    List<UsuarioResponse> toResponseList(List<Usuario> usuarios);
}
