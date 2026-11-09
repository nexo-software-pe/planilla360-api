package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.util.function.Supplier;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Mide cuánto tarda el cálculo de una planilla (la aplicación no conoce la herramienta de métricas). */
public interface MedidorCalculoPort {

    /** Ejecuta el cálculo y registra su duración y su resultado (correcto o con error). */
    <T> T medir(TipoPlanilla tipo, Supplier<T> calculo);
}
