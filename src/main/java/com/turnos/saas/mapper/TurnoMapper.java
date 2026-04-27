package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.entity.Turno;
import com.turnos.saas.model.entity.TurnoHistorial;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TurnoMapper {

    @Mapping(target = "empresaId",        source = "empresa.id")
    @Mapping(target = "cliente.id",       source = "cliente.id")
    @Mapping(target = "cliente.nombre",   source = "cliente.nombre")
    @Mapping(target = "cliente.email",    source = "cliente.email")
    @Mapping(target = "servicio.id",      source = "servicio.id")
    @Mapping(target = "servicio.nombre",  source = "servicio.nombre")
    @Mapping(target = "cotizacion",       ignore = true)
    @Mapping(target = "senia",            ignore = true)
    @Mapping(target = "historial",        ignore = true)
    @Mapping(target = "creadoEn",         source = "createdAt")
    TurnoResponse toResponse(Turno turno);

    @Mapping(target = "fecha",           source = "fechaConfirmada")
    @Mapping(target = "hora",            source = "horaConfirmada")
    @Mapping(target = "nombreCliente",   source = "cliente.nombre")
    @Mapping(target = "nombreServicio",  source = "servicio.nombre")
    TurnoSummary toSummary(Turno turno);

    List<TurnoSummary> toSummaryList(List<Turno> turnos);

    @Mapping(target = "cambiadoPor", expression = "java(historial.getCambiadoPor() != null ? historial.getCambiadoPor().getNombre() : null)")
    TurnoHistorialEntry toHistorialEntry(TurnoHistorial historial);

    List<TurnoHistorialEntry> toHistorialEntryList(List<TurnoHistorial> historial);
}
