package pe.nexosoftware.planilla360.vacaciones.application.port.out;

import pe.nexosoftware.planilla360.vacaciones.PermisoAprobado;
import pe.nexosoftware.planilla360.vacaciones.VacacionesAprobadas;

/** Publica los eventos del módulo para que otros módulos reaccionen. */
public interface PublicadorEventosPort {

    void publicar(VacacionesAprobadas evento);

    void publicar(PermisoAprobado evento);
}
