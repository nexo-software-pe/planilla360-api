package pe.nexosoftware.planilla360.planilla.application.port.out;

import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

/** Datos de la empresa del contexto (los provee el módulo de empresas). */
public interface ConsultaEmpresaPort {

    RegimenLaboral regimenLaboral();
}
