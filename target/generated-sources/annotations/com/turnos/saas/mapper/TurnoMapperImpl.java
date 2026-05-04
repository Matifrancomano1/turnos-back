package com.turnos.saas.mapper;

import com.turnos.saas.dto.response.Responses;
import com.turnos.saas.model.entity.Empresa;
import com.turnos.saas.model.entity.Servicio;
import com.turnos.saas.model.entity.Turno;
import com.turnos.saas.model.entity.TurnoHistorial;
import com.turnos.saas.model.entity.Usuario;
import com.turnos.saas.model.enums.TurnoEstado;
import java.time.LocalDate;
import java.time.LocalTime;
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
public class TurnoMapperImpl implements TurnoMapper {

    @Override
    public Responses.TurnoResponse toResponse(Turno turno) {
        if ( turno == null ) {
            return null;
        }

        Responses.ClienteInfo cliente = null;
        Responses.ServicioInfo servicio = null;
        UUID empresaId = null;
        OffsetDateTime creadoEn = null;
        UUID id = null;
        LocalDate fechaSolicitada = null;
        LocalTime horaSolicitada = null;
        LocalDate fechaConfirmada = null;
        LocalTime horaConfirmada = null;
        TurnoEstado estado = null;
        String observaciones = null;

        cliente = usuarioToClienteInfo( turno.getCliente() );
        servicio = servicioToServicioInfo( turno.getServicio() );
        empresaId = turnoEmpresaId( turno );
        creadoEn = turno.getCreatedAt();
        id = turno.getId();
        fechaSolicitada = turno.getFechaSolicitada();
        horaSolicitada = turno.getHoraSolicitada();
        fechaConfirmada = turno.getFechaConfirmada();
        horaConfirmada = turno.getHoraConfirmada();
        estado = turno.getEstado();
        observaciones = turno.getObservaciones();

        Responses.CotizacionInfo cotizacion = null;
        Responses.SeniaInfo senia = null;
        List<Responses.TurnoHistorialEntry> historial = null;

        Responses.TurnoResponse turnoResponse = new Responses.TurnoResponse( id, empresaId, cliente, servicio, fechaSolicitada, horaSolicitada, fechaConfirmada, horaConfirmada, estado, observaciones, cotizacion, senia, historial, creadoEn );

        return turnoResponse;
    }

    @Override
    public Responses.TurnoSummary toSummary(Turno turno) {
        if ( turno == null ) {
            return null;
        }

        LocalDate fecha = null;
        LocalTime hora = null;
        String nombreCliente = null;
        String nombreServicio = null;
        UUID id = null;
        TurnoEstado estado = null;

        fecha = turno.getFechaConfirmada();
        hora = turno.getHoraConfirmada();
        nombreCliente = turnoClienteNombre( turno );
        nombreServicio = turnoServicioNombre( turno );
        id = turno.getId();
        estado = turno.getEstado();

        Responses.TurnoSummary turnoSummary = new Responses.TurnoSummary( id, fecha, hora, estado, nombreCliente, nombreServicio );

        return turnoSummary;
    }

    @Override
    public List<Responses.TurnoSummary> toSummaryList(List<Turno> turnos) {
        if ( turnos == null ) {
            return null;
        }

        List<Responses.TurnoSummary> list = new ArrayList<Responses.TurnoSummary>( turnos.size() );
        for ( Turno turno : turnos ) {
            list.add( toSummary( turno ) );
        }

        return list;
    }

    @Override
    public Responses.TurnoHistorialEntry toHistorialEntry(TurnoHistorial historial) {
        if ( historial == null ) {
            return null;
        }

        TurnoEstado estadoAnterior = null;
        TurnoEstado estadoNuevo = null;
        String motivo = null;
        OffsetDateTime timestamp = null;

        estadoAnterior = historial.getEstadoAnterior();
        estadoNuevo = historial.getEstadoNuevo();
        motivo = historial.getMotivo();
        timestamp = historial.getTimestamp();

        String cambiadoPor = historial.getCambiadoPor() != null ? historial.getCambiadoPor().getNombre() : null;

        Responses.TurnoHistorialEntry turnoHistorialEntry = new Responses.TurnoHistorialEntry( estadoAnterior, estadoNuevo, motivo, cambiadoPor, timestamp );

        return turnoHistorialEntry;
    }

    @Override
    public List<Responses.TurnoHistorialEntry> toHistorialEntryList(List<TurnoHistorial> historial) {
        if ( historial == null ) {
            return null;
        }

        List<Responses.TurnoHistorialEntry> list = new ArrayList<Responses.TurnoHistorialEntry>( historial.size() );
        for ( TurnoHistorial turnoHistorial : historial ) {
            list.add( toHistorialEntry( turnoHistorial ) );
        }

        return list;
    }

    protected Responses.ClienteInfo usuarioToClienteInfo(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UUID id = null;
        String nombre = null;
        String email = null;

        id = usuario.getId();
        nombre = usuario.getNombre();
        email = usuario.getEmail();

        Responses.ClienteInfo clienteInfo = new Responses.ClienteInfo( id, nombre, email );

        return clienteInfo;
    }

    protected Responses.ServicioInfo servicioToServicioInfo(Servicio servicio) {
        if ( servicio == null ) {
            return null;
        }

        UUID id = null;
        String nombre = null;

        id = servicio.getId();
        nombre = servicio.getNombre();

        Responses.ServicioInfo servicioInfo = new Responses.ServicioInfo( id, nombre );

        return servicioInfo;
    }

    private UUID turnoEmpresaId(Turno turno) {
        if ( turno == null ) {
            return null;
        }
        Empresa empresa = turno.getEmpresa();
        if ( empresa == null ) {
            return null;
        }
        UUID id = empresa.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String turnoClienteNombre(Turno turno) {
        if ( turno == null ) {
            return null;
        }
        Usuario cliente = turno.getCliente();
        if ( cliente == null ) {
            return null;
        }
        String nombre = cliente.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    private String turnoServicioNombre(Turno turno) {
        if ( turno == null ) {
            return null;
        }
        Servicio servicio = turno.getServicio();
        if ( servicio == null ) {
            return null;
        }
        String nombre = servicio.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }
}
