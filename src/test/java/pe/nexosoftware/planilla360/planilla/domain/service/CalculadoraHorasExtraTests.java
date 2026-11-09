package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

@DisplayName("Horas extra")
class CalculadoraHorasExtraTests {

    private static final Dinero BASICO = Dinero.de("3000");
    private static final BigDecimal S1 = new BigDecimal("25");
    private static final BigDecimal S2 = new BigDecimal("35");

    private final CalculadoraHorasExtra calculadora = new CalculadoraHorasExtra();

    @ParameterizedTest(name = "{0} h -> {1}")
    @CsvSource({
        "0, 0.00",
        "0.25, 3.91",
        "1, 15.63",
        "2, 31.25",
        "3, 48.13",
        "4, 65.00",
        "8, 132.50"
    })
    void lasDosPrimerasHorasPagan25PorCientoYLasSiguientes35(String horas, String esperado) {
        assertThat(calculadora.montoDelDia(BASICO, 8, new BigDecimal(horas), S1, S2))
                .isEqualTo(Dinero.de(esperado));
    }

    @Test
    void elValorHoraDependeDeLaJornadaDiaria() {
        assertThat(calculadora.montoDelDia(BASICO, 4, new BigDecimal("2"), S1, S2)).isEqualTo(Dinero.de("62.50"));
    }

    @Test
    void sumaLosDiasRedondeandoCadaUno() {
        List<BigDecimal> dias = List.of(new BigDecimal("0.25"), new BigDecimal("0.25"), new BigDecimal("3"));
        assertThat(calculadora.total(BASICO, 8, dias, S1, S2)).isEqualTo(Dinero.de("55.95"));
    }

    @Test
    void sinHorasElTotalEsCero() {
        assertThat(calculadora.total(BASICO, 8, List.of(), S1, S2)).isEqualTo(Dinero.CERO);
    }

    @Test
    void rechazaHorasNegativas() {
        assertThatThrownBy(() -> calculadora.montoDelDia(BASICO, 8, new BigDecimal("-1"), S1, S2))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("horas extra");
    }

    @Test
    void rechazaBasicoNegativo() {
        assertThatThrownBy(() -> calculadora.montoDelDia(Dinero.de("-1"), 8, BigDecimal.ONE, S1, S2))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("básico");
    }

    @ParameterizedTest
    @CsvSource({"0", "-8"})
    void rechazaJornadaNoPositiva(int jornada) {
        assertThatThrownBy(() -> calculadora.montoDelDia(BASICO, jornada, BigDecimal.ONE, S1, S2))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("jornada");
    }
}
