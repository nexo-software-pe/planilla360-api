package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.port.out.HistorialPlanillaPort;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.LineaPlanillaJpaRepository;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.LineaPlanillaJpaRepository.MontoDelPeriodo;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.LineaPlanillaJpaRepository.SumaPorEmpleado;

/** Lo ya pagado a cada trabajador, leído de las planillas cerradas (los periodos se comparan como texto yyyy-MM). */
@Component
class HistorialPlanillaPersistenceAdapter implements HistorialPlanillaPort {

    /** La última gratificación que importa para una CTS está a lo más a seis meses atrás. */
    private static final int MESES_HACIA_ATRAS_PARA_GRATIFICACION = 7;

    private final LineaPlanillaJpaRepository lineas;

    HistorialPlanillaPersistenceAdapter(LineaPlanillaJpaRepository lineas) {
        this.lineas = lineas;
    }

    @Override
    public Map<UUID, Dinero> ingresosPercibidosEnElAnio(Periodo antesDe) {
        String inicioDelAnio = Periodo.de(antesDe.anio(), 1).toString();
        Map<UUID, Dinero> percibidos = new HashMap<>();
        for (SumaPorEmpleado suma : lineas.ingresosPagados(
                EstadoPlanilla.CERRADA,
                List.of(TipoPlanilla.MENSUAL, TipoPlanilla.GRATIFICACION),
                inicioDelAnio,
                antesDe.toString())) {
            percibidos.put(suma.getEmpleadoId(), Dinero.de(suma.getTotal()));
        }
        return percibidos;
    }

    @Override
    public Map<UUID, List<Dinero>> horasExtraPagadas(Periodo desde, Periodo hasta) {
        Map<UUID, List<Dinero>> porTrabajador = new HashMap<>();
        for (MontoDelPeriodo fila : lineas.horasExtraPagadas(
                EstadoPlanilla.CERRADA, TipoPlanilla.MENSUAL, desde.toString(), hasta.toString())) {
            porTrabajador.computeIfAbsent(fila.getEmpleadoId(), id -> new ArrayList<>())
                    .add(Dinero.de(fila.getMonto()));
        }
        return porTrabajador;
    }

    @Override
    public Map<UUID, Dinero> ultimaGratificacion(Periodo antesDe) {
        String desde = new Periodo(antesDe.valor().minusMonths(MESES_HACIA_ATRAS_PARA_GRATIFICACION)).toString();
        Map<UUID, Dinero> ultima = new HashMap<>();
        // Vienen ordenadas por periodo: la última de cada trabajador es la más reciente.
        for (MontoDelPeriodo fila : lineas.gratificacionesPagadas(
                EstadoPlanilla.CERRADA, TipoPlanilla.GRATIFICACION, desde, antesDe.toString())) {
            ultima.put(fila.getEmpleadoId(), Dinero.de(fila.getMonto()));
        }
        return ultima;
    }
}
