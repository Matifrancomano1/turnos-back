package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.Rol;
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
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public Responses.UsuarioResponse toResponse(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        OffsetDateTime creadoEn = null;
        UUID id = null;
        String nombre = null;
        String email = null;
        String telefono = null;
        Rol rol = null;
        Boolean activo = null;

        creadoEn = usuario.getCreatedAt();
        id = usuario.getId();
        nombre = usuario.getNombre();
        email = usuario.getEmail();
        telefono = usuario.getTelefono();
        rol = usuario.getRol();
        activo = usuario.getActivo();

        UUID empresaId = usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null;

        Responses.UsuarioResponse usuarioResponse = new Responses.UsuarioResponse( id, nombre, email, telefono, rol, empresaId, activo, creadoEn );

        return usuarioResponse;
    }

    @Override
    public Responses.UsuarioInfo toInfo(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UUID id = null;
        String nombre = null;
        String email = null;
        Rol rol = null;

        id = usuario.getId();
        nombre = usuario.getNombre();
        email = usuario.getEmail();
        rol = usuario.getRol();

        UUID empresaId = usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null;

        Responses.UsuarioInfo usuarioInfo = new Responses.UsuarioInfo( id, nombre, email, rol, empresaId );

        return usuarioInfo;
    }

    @Override
    public List<Responses.UsuarioResponse> toResponseList(List<Usuario> usuarios) {
        if ( usuarios == null ) {
            return null;
        }

        List<Responses.UsuarioResponse> list = new ArrayList<Responses.UsuarioResponse>( usuarios.size() );
        for ( Usuario usuario : usuarios ) {
            list.add( toResponse( usuario ) );
        }

        return list;
    }
}
