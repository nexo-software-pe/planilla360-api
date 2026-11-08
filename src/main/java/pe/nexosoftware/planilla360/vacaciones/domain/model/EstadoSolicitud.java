package pe.nexosoftware.planilla360.vacaciones.domain.model;

/**
 * Estados de una solicitud de vacaciones (patrón State). Cada estado sabe a cuáles puede pasar:
 *
 * <pre>
 * SOLICITADA -> APROBADA_JEFE -> APROBADA_RRHH -> EN_GOCE -> FINALIZADA
 *      |             |
 *      +--> RECHAZADA / CANCELADA
 * </pre>
 */
public enum EstadoSolicitud {
    SOLICITADA,
    APROBADA_JEFE,
    APROBADA_RRHH,
    EN_GOCE,
    FINALIZADA,
    RECHAZADA,
    CANCELADA;

    public boolean puedePasarA(EstadoSolicitud destino) {
        return switch (this) {
            case SOLICITADA -> destino == APROBADA_JEFE || destino == RECHAZADA || destino == CANCELADA;
            case APROBADA_JEFE -> destino == APROBADA_RRHH || destino == RECHAZADA || destino == CANCELADA;
            case APROBADA_RRHH -> destino == EN_GOCE;
            case EN_GOCE -> destino == FINALIZADA;
            case FINALIZADA, RECHAZADA, CANCELADA -> false;
        };
    }

    /** Una solicitud activa reserva sus días: todavía pueden consumirse del saldo. */
    public boolean reservaDias() {
        return this == SOLICITADA || this == APROBADA_JEFE || this == APROBADA_RRHH || this == EN_GOCE;
    }

    public boolean esTerminal() {
        return this == FINALIZADA || this == RECHAZADA || this == CANCELADA;
    }
}
