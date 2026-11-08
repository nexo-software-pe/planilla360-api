package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

@DisplayName("Semestre de beneficio")
class SemestreDeBeneficioTests {

    @Test
    void laGratificacionDeJulioComputaEneroAJunio() {
        assertThat(SemestreDeBeneficio.deGratificacion(Periodo.de(2026, 7)))
                .isEqualTo(new SemestreDeBeneficio(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)));
    }

    @Test
    void laGratificacionDeDiciembreComputaJulioADiciembre() {
        assertThat(SemestreDeBeneficio.deGratificacion(Periodo.de(2026, 12)))
                .isEqualTo(new SemestreDeBeneficio(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)));
    }

    @Test
    void laCtsDeMayoComputaNoviembreAAbrilDelAnioAnterior() {
        assertThat(SemestreDeBeneficio.deCts(Periodo.de(2026, 5)))
                .isEqualTo(new SemestreDeBeneficio(LocalDate.of(2025, 11, 1), LocalDate.of(2026, 4, 30)));
    }

    @Test
    void laCtsDeNoviembreComputaMayoAOctubre() {
        assertThat(SemestreDeBeneficio.deCts(Periodo.de(2026, 11)))
                .isEqualTo(new SemestreDeBeneficio(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 10, 31)));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 8, 9, 10, 11})
    void soloJulioYDiciembrePaganGratificacion(int mes) {
        assertThatThrownBy(() -> SemestreDeBeneficio.deGratificacion(Periodo.de(2026, mes)))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("julio o diciembre");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 6, 7, 8, 9, 10, 12})
    void soloMayoYNoviembreDepositanCts(int mes) {
        assertThatThrownBy(() -> SemestreDeBeneficio.deCts(Periodo.de(2026, mes)))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("mayo o noviembre");
    }
}
