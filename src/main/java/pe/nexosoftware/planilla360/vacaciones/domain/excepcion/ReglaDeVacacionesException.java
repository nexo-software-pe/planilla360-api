package pe.nexosoftware.planilla360.vacaciones.domain.excepcion;

import pe.nexosoftware.planilla360.compartido.dominio.ReglaDeDominioException;

/** Se incumplió una regla de negocio de vacaciones o permisos. La capa web la traduce a {@code problem+json}. */
public class ReglaDeVacacionesException extends ReglaDeDominioException {

    public ReglaDeVacacionesException(String mensaje) {
        super(mensaje);
    }
}
