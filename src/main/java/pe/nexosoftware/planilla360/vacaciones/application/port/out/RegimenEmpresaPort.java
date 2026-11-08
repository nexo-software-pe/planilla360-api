package pe.nexosoftware.planilla360.vacaciones.application.port.out;

import pe.nexosoftware.planilla360.vacaciones.domain.model.PoliticaVacaciones;

public interface RegimenEmpresaPort {

    /** Política de vacaciones según el régimen laboral de la empresa del contexto. */
    PoliticaVacaciones politicaDeLaEmpresa();
}
