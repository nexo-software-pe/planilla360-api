package pe.nexosoftware.planilla360.planilla.application.port.in;

import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.planilla.application.model.FiltroPlanillas;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

/** Crear, calcular, simular, consultar y validar planillas (mensual, gratificación y CTS). */
public interface GestionarPlanillasUseCase {

    /** Crea una planilla en borrador. Falla si ya hay una vigente (no anulada) del mismo periodo y tipo. */
    Planilla crear(Periodo periodo, TipoPlanilla tipo);

    /** Calcula (o recalcula) las líneas de una planilla en borrador o calculada. */
    Planilla calcular(UUID planillaId);

    /** Calcula sin guardar nada: sirve para ver cuánto costaría una gratificación o una CTS antes de generarla. */
    Planilla simular(Periodo periodo, TipoPlanilla tipo);

    /** Crea y calcula en un solo paso (la generación de una gratificación o una CTS). */
    Planilla generar(Periodo periodo, TipoPlanilla tipo);

    Planilla obtener(UUID planillaId);

    Pagina<ResumenPlanilla> listar(FiltroPlanillas filtro, int pagina, int tamano);

    /** Revisa si la planilla se puede cerrar y reúne todas las incidencias. */
    ResultadoValidacion validar(UUID planillaId);
}
