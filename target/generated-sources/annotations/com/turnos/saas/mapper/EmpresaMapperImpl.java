package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses;
import com.turnos.saas.dto.response.Responses.EmpresaConfigResponse;
import com.turnos.saas.model.entity.Empresa;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-13T17:36:27-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class EmpresaMapperImpl implements EmpresaMapper {

    @Override
    public Responses.EmpresaResponse toResponse(Empresa empresa) {
        if ( empresa == null ) {
            return null;
        }

        OffsetDateTime creadoEn = null;
        String slug = null;
        UUID id = null;
        String nombre = null;
        String emailContacto = null;
        String direccion = null;
        String telefono = null;
        Boolean activa = null;

        creadoEn = empresa.getCreatedAt();
        slug = empresa.getSlug();
        id = empresa.getId();
        nombre = empresa.getNombre();
        emailContacto = empresa.getEmailContacto();
        direccion = empresa.getDireccion();
        telefono = empresa.getTelefono();
        activa = empresa.getActiva();

        Responses.EmpresaConfigResponse config = new EmpresaConfigResponse(empresa.getHoraApertura(), empresa.getHoraCierre(), empresa.getDuracionSlotMinutos(), empresa.getSabadoHabilitado(), empresa.getDomingoHabilitado());

        Responses.EmpresaResponse empresaResponse = new Responses.EmpresaResponse( id, nombre, slug, emailContacto, direccion, telefono, config, activa, creadoEn );

        return empresaResponse;
    }

    @Override
    public List<Responses.EmpresaResponse> toResponseList(List<Empresa> empresas) {
        if ( empresas == null ) {
            return null;
        }

        List<Responses.EmpresaResponse> list = new ArrayList<Responses.EmpresaResponse>( empresas.size() );
        for ( Empresa empresa : empresas ) {
            list.add( toResponse( empresa ) );
        }

        return list;
    }
}
