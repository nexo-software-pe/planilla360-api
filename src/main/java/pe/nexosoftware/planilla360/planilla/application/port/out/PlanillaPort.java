package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.planilla.application.model.FiltroPlanillas;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Guarda y consulta planillas de la empresa del contexto. */
public interface PlanillaPort {

    /** Inserta la planilla o actualiza la existente (las líneas se reemplazan mientras no esté cerrada). */
    Planilla guardar(Planilla planilla);

    Optional<Planilla> buscar(UUID id);

    /** Como {@link #buscar}, pero bloquea la fila hasta el fin de la transacción para serializar los cambios. */
    Optional<Planilla> buscarParaActualizar(UUID id);

    /** Planilla de la empresa que ya usó esa clave de idempotencia. */
    Optional<UUID> idConClave(String clave);

    /** Hay una planilla borrador, calculada o cerrada del periodo y tipo (las anuladas no cuentan). */
    boolean existeVigente(Periodo periodo, TipoPlanilla tipo);

    /** Hay una planilla cerrada del periodo y tipo distinta de la indicada. */
    boolean existeCerradaDistinta(Periodo periodo, TipoPlanilla tipo, UUID planillaId);

    Pagina<ResumenPlanilla> listar(FiltroPlanillas filtro, int pagina, int tamano);
}
