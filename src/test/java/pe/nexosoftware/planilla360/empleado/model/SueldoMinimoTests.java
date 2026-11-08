package pe.nexosoftware.planilla360.empleado.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SueldoMinimoTests {

    private static final BigDecimal RMV = new BigDecimal("1130.00");

    @Test
    void jornadaCompletaExigeLaRmvCompleta() {
        assertThat(SueldoMinimo.para(RMV, 48)).isEqualByComparingTo("1130.00");
    }

    @Test
    void conVeinticuatroHorasSigueSiendoLaRmvCompleta() {
        assertThat(SueldoMinimo.para(RMV, 24)).isEqualByComparingTo("1130.00");
    }

    @Test
    void conVeintitresHorasEsProporcionalSobreCuarentaYOcho() {
        // 1130 × 23 / 48 = 541.4583… → 541.46
        assertThat(SueldoMinimo.para(RMV, 23)).isEqualByComparingTo("541.46");
    }

    @ParameterizedTest
    @CsvSource({"1,23.54", "12,282.50", "20,470.83"})
    void tiempoParcialEsProporcionalYSeRedondeaADosDecimales(int horas, String esperado) {
        assertThat(SueldoMinimo.para(RMV, horas)).isEqualByComparingTo(esperado);
    }

    @Test
    void laJornadaDebeEstarEntreUnaYCuarentaYOchoHoras() {
        assertThatThrownBy(() -> SueldoMinimo.para(RMV, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SueldoMinimo.para(RMV, 49)).isInstanceOf(IllegalArgumentException.class);
    }
}
