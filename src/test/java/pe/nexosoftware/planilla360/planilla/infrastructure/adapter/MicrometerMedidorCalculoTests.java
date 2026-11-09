package pe.nexosoftware.planilla360.planilla.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

@DisplayName("MicrometerMedidorCalculo")
class MicrometerMedidorCalculoTests {

    private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
    private final MicrometerMedidorCalculo medidor = new MicrometerMedidorCalculo(registro);

    @Test
    void devuelveElResultadoDelCalculoYRegistraUnaMedicionCorrecta() {
        String valor = medidor.medir(TipoPlanilla.MENSUAL, () -> "lineas");

        assertThat(valor).isEqualTo("lineas");
        assertThat(registro.get("planilla.calculo.duracion")
                .tag("tipo", "MENSUAL").tag("resultado", "ok").timer().count()).isEqualTo(1);
    }

    @Test
    void siElCalculoFallaRegistraLaMedicionConResultadoErrorYRelanzaLaExcepcion() {
        assertThatThrownBy(() -> medidor.medir(TipoPlanilla.CTS, () -> {
            throw new IllegalStateException("falla a propósito");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(registro.get("planilla.calculo.duracion")
                .tag("tipo", "CTS").tag("resultado", "error").timer().count()).isEqualTo(1);
    }

    @Test
    void cadaTipoDePlanillaTieneSuPropiaSerie() {
        medidor.medir(TipoPlanilla.MENSUAL, () -> 1);
        medidor.medir(TipoPlanilla.MENSUAL, () -> 2);
        medidor.medir(TipoPlanilla.GRATIFICACION, () -> 3);

        assertThat(registro.get("planilla.calculo.duracion").tag("tipo", "MENSUAL").timer().count()).isEqualTo(2);
        assertThat(registro.get("planilla.calculo.duracion").tag("tipo", "GRATIFICACION").timer().count())
                .isEqualTo(1);
    }
}
