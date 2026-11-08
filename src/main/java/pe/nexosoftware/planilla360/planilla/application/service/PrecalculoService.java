package pe.nexosoftware.planilla360.planilla.application.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.port.in.GestionarPlanillasUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.in.PrecalcularPlanillaUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaCatalogoPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.service.CalendarioLaboral;

/**
 * El último día hábil del mes deja lista, en borrador, la planilla mensual de todas las empresas. Es idempotente: si la
 * empresa ya tiene una planilla vigente del mes (aunque la haya creado RRHH a mano) no se toca.
 */
@Service
public class PrecalculoService implements PrecalcularPlanillaUseCase {

    private static final int MES_GRATIFICACION_JULIO = 7;
    private static final int MES_GRATIFICACION_DICIEMBRE = 12;
    private static final int MES_CTS_MAYO = 5;
    private static final int MES_CTS_NOVIEMBRE = 11;

    private final GestionarPlanillasUseCase planillas;
    private final PlanillaPort repositorio;
    private final ConsultaCatalogoPort catalogo;
    private final CalendarioLaboral calendario = new CalendarioLaboral();

    public PrecalculoService(GestionarPlanillasUseCase planillas, PlanillaPort repositorio,
            ConsultaCatalogoPort catalogo) {
        this.planillas = planillas;
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    @Override
    @Transactional
    public Optional<UUID> precalcularSiCorresponde(LocalDate hoy) {
        if (!calendario.esUltimoDiaHabil(hoy, catalogo::esFeriado)) {
            return Optional.empty();
        }
        Periodo periodo = Periodo.de(hoy);
        if (repositorio.existeVigente(periodo, TipoPlanilla.MENSUAL)) {
            return Optional.empty();
        }
        return Optional.of(planillas.generar(periodo, TipoPlanilla.MENSUAL).id());
    }

    @Override
    public Optional<String> recordatorioDeBeneficio(LocalDate hoy) {
        int mes = hoy.getMonthValue();
        if (mes == MES_GRATIFICACION_JULIO || mes == MES_GRATIFICACION_DICIEMBRE) {
            return Optional.of("Este mes se paga la gratificación (" + Periodo.de(hoy) + "): genere la planilla de "
                    + "gratificación y cierre antes del 15.");
        }
        if (mes == MES_CTS_MAYO || mes == MES_CTS_NOVIEMBRE) {
            return Optional.of("Este mes se deposita la CTS (" + Periodo.de(hoy) + "): genere la planilla de CTS "
                    + "y deposite antes del 15.");
        }
        return Optional.empty();
    }
}
