package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

@DisplayName("EstadoPlanilla (State)")
class EstadoPlanillaTests {

    @ParameterizedTest(name = "calcular desde {0} -> {1}")
    @CsvSource({"BORRADOR, CALCULADA", "CALCULADA, CALCULADA"})
    void permiteCalcularYRecalcularAntesDeCerrar(EstadoPlanilla origen, EstadoPlanilla destino) {
        assertThat(origen.alCalcular()).isEqualTo(destino);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPlanilla.class, names = {"CERRADA", "ANULADA"})
    void noSePuedeCalcularUnaPlanillaCerradaOAnulada(EstadoPlanilla estado) {
        assertThatThrownBy(estado::alCalcular).isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining(estado.name());
    }

    @Test
    void soloUnaPlanillaCalculadaPuedeCerrarse() {
        assertThat(EstadoPlanilla.CALCULADA.alCerrar()).isEqualTo(EstadoPlanilla.CERRADA);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPlanilla.class, names = {"BORRADOR", "CERRADA", "ANULADA"})
    void noSePuedeCerrarDesdeOtroEstado(EstadoPlanilla estado) {
        assertThatThrownBy(estado::alCerrar).isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining(estado.name());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPlanilla.class, names = {"BORRADOR", "CALCULADA", "CERRADA"})
    void sePuedeAnularMientrasNoEstePagada(EstadoPlanilla estado) {
        assertThat(estado.alAnular()).isEqualTo(EstadoPlanilla.ANULADA);
    }

    @Test
    void unaPlanillaAnuladaEsDefinitiva() {
        assertThatThrownBy(EstadoPlanilla.ANULADA::alAnular).isInstanceOf(ReglaDePlanillaException.class);
    }

    @ParameterizedTest(name = "{0} permite modificar líneas: {1}")
    @CsvSource({"BORRADOR, true", "CALCULADA, true", "CERRADA, false", "ANULADA, false"})
    void soloSeModificanLasLineasAntesDeCerrar(EstadoPlanilla estado, boolean permite) {
        assertThat(estado.permiteModificarLineas()).isEqualTo(permite);
    }
}
