package pe.nexosoftware.planilla360.planilla.infrastructure.adapter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.planilla.application.port.out.MedidorCalculoPort;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/**
 * Adapter de {@link MedidorCalculoPort} sobre Micrometer: el temporizador {@code planilla.calculo.duracion}
 * (en Prometheus, {@code planilla_calculo_duracion_seconds_*}) con las etiquetas {@code tipo} y {@code resultado}.
 * Mide el armado de las líneas (lectura de datos y cálculo), no el guardado.
 */
@Component
class MicrometerMedidorCalculo implements MedidorCalculoPort {

    static final String NOMBRE = "planilla.calculo.duracion";

    private final MeterRegistry registro;

    MicrometerMedidorCalculo(MeterRegistry registro) {
        this.registro = registro;
    }

    @Override
    public <T> T medir(TipoPlanilla tipo, Supplier<T> calculo) {
        long inicio = System.nanoTime();
        String resultado = "error";
        try {
            T valor = calculo.get();
            resultado = "ok";
            return valor;
        } finally {
            Timer.builder(NOMBRE)
                    .description("Duración del cálculo de las líneas de una planilla")
                    .tag("tipo", tipo.name())
                    .tag("resultado", resultado)
                    .publishPercentileHistogram()
                    .register(registro)
                    .record(System.nanoTime() - inicio, TimeUnit.NANOSECONDS);
        }
    }
}
