package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

@DisplayName("Descuentos por asistencia")
class CalculadoraDescuentoAsistenciaTests {

    private final CalculadoraDescuentoAsistencia calculadora = new CalculadoraDescuentoAsistencia();

    @ParameterizedTest(name = "básico {0}, {1} min, jornada {2} h -> {3}")
    @CsvSource({
        "3000, 30, 8, 6.25",
        "3000, 1, 8, 0.21",
        "1130, 15, 8, 1.18",
        "3000, 30, 6, 8.33",
        "3000, 0, 8, 0.00",
        "0, 30, 8, 0.00"
    })
    void descuentaLosMinutosAlValorMinuto(String basico, int minutos, int jornada, String esperado) {
        assertThat(calculadora.porTardanza(Dinero.de(basico), minutos, jornada)).isEqualTo(Dinero.de(esperado));
    }

    @ParameterizedTest(name = "básico {0}, {1} días -> {2}")
    @CsvSource({"3000, 2, 200.00", "1130, 1, 37.67", "3000, 0, 0.00", "3000, 30, 3000.00"})
    void descuentaLosDiasAlValorDia(String basico, int dias, String esperado) {
        assertThat(calculadora.porInasistencias(Dinero.de(basico), dias)).isEqualTo(Dinero.de(esperado));
    }

    @Test
    void elTotalSumaTardanzasEInasistencias() {
        assertThat(calculadora.total(Dinero.de("3000"), 30, 2, 8)).isEqualTo(Dinero.de("206.25"));
    }

    @Test
    void nuncaSeDescuentaMasQueElBasico() {
        assertThat(calculadora.porInasistencias(Dinero.de("3000"), 45)).isEqualTo(Dinero.de("3000.00"));
        assertThat(calculadora.total(Dinero.de("3000"), 100_000, 30, 8)).isEqualTo(Dinero.de("3000.00"));
    }

    @Test
    void rechazaValoresInvalidos() {
        Dinero basico = Dinero.de("3000");
        assertThatThrownBy(() -> calculadora.porTardanza(basico, -1, 8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.porTardanza(basico, 10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.porInasistencias(basico, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.porInasistencias(Dinero.de("-1"), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
