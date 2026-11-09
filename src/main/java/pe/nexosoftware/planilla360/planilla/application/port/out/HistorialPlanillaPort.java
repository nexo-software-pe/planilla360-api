package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;

/** Lo ya pagado a cada trabajador en planillas cerradas (solo cuentan las cerradas). */
public interface HistorialPlanillaPort {

    /** Ingresos de las planillas mensuales y de gratificación cerradas del año del periodo, anteriores a él. */
    Map<UUID, Dinero> ingresosPercibidosEnElAnio(Periodo antesDe);

    /** Horas extra pagadas en cada planilla mensual cerrada entre los periodos (inclusive), en orden cronológico. */
    Map<UUID, List<Dinero>> horasExtraPagadas(Periodo desde, Periodo hasta);

    /** Última gratificación cerrada anterior al periodo (solo el monto, sin la bonificación). */
    Map<UUID, Dinero> ultimaGratificacion(Periodo antesDe);
}
