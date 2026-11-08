package pe.nexosoftware.planilla360.planilla.application.service;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaAsistenciaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaVacacionesPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.service.ArmadorDeLineas.RangoDelPersonal;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.ContextoCierre;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;
import pe.nexosoftware.planilla360.planilla.domain.validation.ValidadorCierre;

/**
 * Arma el contexto que el dominio no posee (personal sin régimen, asistencia, vacaciones y otras planillas) y corre la
 * cadena de validaciones de cierre. La asistencia y las vacaciones solo condicionan la planilla mensual.
 */
@Component
class ValidadorDePlanilla {

    private final PersonalDelPeriodo personalDelPeriodo;
    private final ConsultaAsistenciaPort asistencia;
    private final ConsultaVacacionesPort vacaciones;
    private final PlanillaPort planillas;
    private final ValidadorCierre validador = ValidadorCierre.estandar();

    ValidadorDePlanilla(
            PersonalDelPeriodo personalDelPeriodo,
            ConsultaAsistenciaPort asistencia,
            ConsultaVacacionesPort vacaciones,
            PlanillaPort planillas) {
        this.personalDelPeriodo = personalDelPeriodo;
        this.asistencia = asistencia;
        this.vacaciones = vacaciones;
        this.planillas = planillas;
    }

    ResultadoValidacion validar(Planilla planilla) {
        boolean esMensual = planilla.tipo() == TipoPlanilla.MENSUAL;
        Periodo periodo = planilla.periodo();
        RangoDelPersonal rango = ArmadorDeLineas.rangoDelPersonal(periodo, planilla.tipo());
        Set<UUID> sinRegimen = personalDelPeriodo.resolver(rango.desde(), rango.hasta()).sinRegimen();
        ContextoCierre contexto = new ContextoCierre(
                planilla,
                sinRegimen,
                !esMensual || asistencia.mesCompleto(periodo),
                esMensual ? vacaciones.solicitudesPendientesQueSolapan(periodo) : 0,
                planillas.existeCerradaDistinta(periodo, planilla.tipo(), planilla.id()));
        return validador.validar(contexto);
    }
}
