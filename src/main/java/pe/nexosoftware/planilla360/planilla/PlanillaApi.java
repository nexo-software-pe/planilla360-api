package pe.nexosoftware.planilla360.planilla;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de planilla: lo que otros módulos necesitan leer de una planilla ya calculada. */
public interface PlanillaApi {

    /** Boletas (una por trabajador) de una planilla de la empresa del contexto; vacío si no existe. */
    Optional<DatosParaBoletas> datosParaBoletas(UUID planillaId);

    /**
     * @param estado BORRADOR, CALCULADA, CERRADA o ANULADA
     * @param tipo MENSUAL, GRATIFICACION o CTS
     */
    record DatosParaBoletas(UUID planillaId, YearMonth periodo, String tipo, String estado, List<LineaBoleta> lineas) {
    }

    record LineaBoleta(
            UUID empleadoId,
            String regimenPensionario,
            List<ConceptoBoleta> conceptos,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal neto) {
    }

    /** @param tipo INGRESO, DESCUENTO o APORTE_EMPLEADOR */
    record ConceptoBoleta(String codigo, String descripcion, String tipo, BigDecimal monto) {
    }
}
