package pe.nexosoftware.planilla360.asistencia.domain.excepcion;

import pe.nexosoftware.planilla360.compartido.dominio.ReglaDeDominioException;

/** Se incumplió una regla de negocio de asistencia. La capa web la traduce a {@code problem+json}. */
public class ReglaDeAsistenciaException extends ReglaDeDominioException {

    public ReglaDeAsistenciaException(String mensaje) {
        super(mensaje);
    }
}
