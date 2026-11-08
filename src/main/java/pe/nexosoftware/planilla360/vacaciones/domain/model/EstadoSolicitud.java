package pe.nexosoftware.planilla360.vacaciones.domain.model;

/** Estados de una solicitud de vacaciones (patrón State). (Esqueleto del commit ROJO.) */
public enum EstadoSolicitud {
    SOLICITADA,
    APROBADA_JEFE,
    APROBADA_RRHH,
    EN_GOCE,
    FINALIZADA,
    RECHAZADA,
    CANCELADA;

    public boolean puedePasarA(EstadoSolicitud destino) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Una solicitud activa reserva sus días: todavía pueden consumirse del saldo. */
    public boolean reservaDias() {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean esTerminal() {
        throw new UnsupportedOperationException("pendiente");
    }
}
