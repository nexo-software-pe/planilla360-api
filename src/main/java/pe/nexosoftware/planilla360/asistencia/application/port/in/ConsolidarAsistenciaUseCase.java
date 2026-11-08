package pe.nexosoftware.planilla360.asistencia.application.port.in;

import java.time.LocalDate;

public interface ConsolidarAsistenciaUseCase {

    /**
     * Consolida la asistencia de la fecha para todos los empleados con horario de la empresa del
     * contexto. Es repetible: volver a ejecutarla reemplaza el resultado anterior.
     *
     * @return cantidad de empleados procesados
     */
    int consolidar(LocalDate fecha);
}
