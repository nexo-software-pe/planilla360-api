package pe.nexosoftware.planilla360.vacaciones.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PoliticaVacaciones;
import pe.nexosoftware.planilla360.vacaciones.domain.service.CalculadoraPeriodos.PeriodoCalculado;

class CalculadoraPeriodosTests {

    private static final LocalDate INGRESO = LocalDate.of(2024, 3, 1);

    private final CalculadoraPeriodos calculadora = new CalculadoraPeriodos();

    private List<PeriodoCalculado> calcular(LocalDate ingreso, LocalDate cese, String hoy, PoliticaVacaciones p) {
        return calculadora.periodosCumplidos(ingreso, cese, LocalDate.parse(hoy), p);
    }

    @ParameterizedTest(name = "al {0} lleva {1} periodos")
    @CsvSource({
        "2024-03-01, 0",
        "2025-02-27, 0",
        "2025-02-28, 0",
        "2025-03-01, 1",
        "2026-02-28, 1",
        "2026-03-01, 2",
        "2027-03-01, 3",
        "2023-01-01, 0"
    })
    void unPeriodoSeGanaAlCumplirseElAnioDeServicio(String hoy, int cantidad) {
        assertThat(calcular(INGRESO, null, hoy, PoliticaVacaciones.general())).hasSize(cantidad);
    }

    @Test
    void elPrimerPeriodoVaDelIngresoAlDiaAnteriorDelAniversario() {
        List<PeriodoCalculado> periodos = calcular(INGRESO, null, "2025-03-01", PoliticaVacaciones.general());

        assertThat(periodos).containsExactly(
                new PeriodoCalculado(LocalDate.of(2024, 3, 1), LocalDate.of(2025, 2, 28), 30));
    }

    @Test
    void losPeriodosSonConsecutivosSinHuecosNiSolapes() {
        List<PeriodoCalculado> periodos = calcular(INGRESO, null, "2027-06-15", PoliticaVacaciones.general());

        assertThat(periodos).extracting(PeriodoCalculado::desde).containsExactly(
                LocalDate.of(2024, 3, 1), LocalDate.of(2025, 3, 1), LocalDate.of(2026, 3, 1));
        assertThat(periodos).extracting(PeriodoCalculado::hasta).containsExactly(
                LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28), LocalDate.of(2027, 2, 28));
    }

    @Test
    void losDiasGanadosDependenDeLaPolitica() {
        assertThat(calcular(INGRESO, null, "2026-03-01", PoliticaVacaciones.general()))
                .extracting(PeriodoCalculado::diasGanados).containsExactly(30, 30);
        assertThat(calcular(INGRESO, null, "2026-03-01", PoliticaVacaciones.pequenaEmpresa()))
                .extracting(PeriodoCalculado::diasGanados).containsExactly(15, 15);
    }

    @Test
    void unIngresoEn29DeFebreroGanaElPrimerPeriodoEl28DeFebreroDelAnioSiguiente() {
        LocalDate bisiesto = LocalDate.of(2024, 2, 29);

        assertThat(calcular(bisiesto, null, "2025-02-27", PoliticaVacaciones.general())).isEmpty();
        assertThat(calcular(bisiesto, null, "2025-02-28", PoliticaVacaciones.general())).hasSize(1);
        assertThat(calcular(bisiesto, null, "2028-02-28", PoliticaVacaciones.general())).hasSize(3);
        assertThat(calcular(bisiesto, null, "2028-02-29", PoliticaVacaciones.general())).hasSize(4);
    }

    @Test
    void unEmpleadoCesadoNoGanaPeriodosDespuesDeSuCese() {
        LocalDate cese = LocalDate.of(2025, 6, 30);

        assertThat(calcular(INGRESO, cese, "2030-01-01", PoliticaVacaciones.general())).hasSize(1);
        assertThat(calcular(INGRESO, LocalDate.of(2025, 2, 28), "2030-01-01", PoliticaVacaciones.general())).isEmpty();
        assertThat(calcular(INGRESO, LocalDate.of(2025, 3, 1), "2030-01-01", PoliticaVacaciones.general())).hasSize(1);
    }

    @Test
    void siElCeseEsPosteriorAHoyManda() {
        assertThat(calcular(INGRESO, LocalDate.of(2040, 1, 1), "2026-03-01", PoliticaVacaciones.general())).hasSize(2);
    }
}
