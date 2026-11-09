package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Jornada laboral diaria")
class JornadaLaboralTests {

    @ParameterizedTest(name = "{0} h semanales en {1} días -> {2} h diarias")
    @CsvSource({
        "48, 6, 8", "36, 6, 6", "40, 5, 8", "30, 5, 6", "20, 4, 5", "24, 5, 5", "48, 5, 8", "1, 7, 1", "44, 5, 8"})
    void sinHorarioDivideLaJornadaSemanalEntreLosDiasLaborablesDelContrato(int semanal, int dias, int diarias) {
        assertThat(JornadaLaboral.horasDiarias(null, semanal, dias)).isEqualTo(diarias);
    }

    @ParameterizedTest(name = "horario de {0} min efectivos -> {1} h diarias")
    @CsvSource({"480, 8", "540, 8", "720, 8", "450, 8", "360, 6", "361, 7", "60, 1", "1, 1"})
    void conHorarioUsaLosMinutosEfectivosYTopeDeOchoHoras(int minutos, int diarias) {
        assertThat(JornadaLaboral.horasDiarias(minutos, 48, 6)).isEqualTo(diarias);
    }

    @Test
    void elHorarioPrevaleceSobreLaJornadaDelContrato() {
        assertThat(JornadaLaboral.horasDiarias(240, 48, 6)).isEqualTo(4);
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "49"})
    void sinHorarioLaJornadaSemanalDebeEstarEntre1Y48(int semanal) {
        assertThatThrownBy(() -> JornadaLaboral.horasDiarias(null, semanal, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("jornada semanal");
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "8"})
    void sinHorarioLosDiasLaborablesDebenEstarEntre1Y7(int dias) {
        assertThatThrownBy(() -> JornadaLaboral.horasDiarias(null, 40, dias))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("días laborables");
    }

    @ParameterizedTest
    @CsvSource({"0", "-30"})
    void unHorarioSinMinutosEfectivosEsInvalido(int minutos) {
        assertThatThrownBy(() -> JornadaLaboral.horasDiarias(minutos, 48, 6))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("horario");
    }

    @Test
    void conHorarioNoSeValidaLaJornadaSemanal() {
        assertThat(JornadaLaboral.horasDiarias(480, 0, 0)).isEqualTo(8);
    }
}
