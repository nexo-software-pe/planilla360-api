package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.planilla.domain.model.SemestreDeBeneficio;
import pe.nexosoftware.planilla360.planilla.domain.model.TiempoComputable;

@DisplayName("Tiempo de servicio computable")
class CalculadoraTiempoDeServicioTests {

    private static final SemestreDeBeneficio ENE_JUN = new SemestreDeBeneficio(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    private static final SemestreDeBeneficio JUL_DIC = new SemestreDeBeneficio(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

    private final CalculadoraTiempoDeServicio calculadora = new CalculadoraTiempoDeServicio();

    @Test
    void quienIngresoAntesDelSemestreComputaLosSeisMeses() {
        assertThat(calculadora.computar(LocalDate.of(2024, 3, 10), null, ENE_JUN))
                .isEqualTo(new TiempoComputable(6, 0));
    }

    @Test
    void quienIngresoElPrimerDiaDelSemestreComputaLosSeisMeses() {
        assertThat(calculadora.computar(LocalDate.of(2026, 7, 1), null, JUL_DIC))
                .isEqualTo(new TiempoComputable(6, 0));
    }

    @ParameterizedTest(name = "ingreso {0} -> {1} meses y {2} días")
    @CsvSource({
        "2026-03-15, 3, 16",
        "2026-06-01, 1, 0",
        "2026-06-30, 0, 1",
        "2026-02-01, 5, 0",
        "2026-01-31, 5, 1"
    })
    void quienIngresaDentroDelSemestreComputaDesdeSuIngreso(LocalDate ingreso, int meses, int dias) {
        assertThat(calculadora.computar(ingreso, null, ENE_JUN)).isEqualTo(new TiempoComputable(meses, dias));
    }

    @Test
    void quienCesaDentroDelSemestreComputaHastaSuCese() {
        assertThat(calculadora.computar(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 4, 14), ENE_JUN))
                .isEqualTo(new TiempoComputable(3, 14));
    }

    @Test
    void treintaDiasSeConviertenEnUnMes() {
        assertThat(calculadora.computar(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31), JUL_DIC))
                .isEqualTo(new TiempoComputable(1, 0));
    }

    @Test
    void quienCesoAntesDelSemestreNoComputa() {
        assertThat(calculadora.computar(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 12, 31), ENE_JUN))
                .isEqualTo(new TiempoComputable(0, 0));
    }

    @Test
    void quienIngresaDespuesDelSemestreNoComputa() {
        assertThat(calculadora.computar(LocalDate.of(2026, 7, 1), null, ENE_JUN))
                .isEqualTo(new TiempoComputable(0, 0));
    }

    @Test
    void elCeseDespuesDelSemestreNoReduceElTiempo() {
        assertThat(calculadora.computar(LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), ENE_JUN))
                .isEqualTo(new TiempoComputable(6, 0));
    }
}
