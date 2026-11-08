package pe.nexosoftware.planilla360.asistencia.domain.excepcion;

/** Se incumplió una regla de negocio de asistencia. La capa web la traduce a {@code problem+json}. */
public class ReglaDeAsistenciaException extends RuntimeException {

    public ReglaDeAsistenciaException(String mensaje) {
        super(mensaje);
    }
}
