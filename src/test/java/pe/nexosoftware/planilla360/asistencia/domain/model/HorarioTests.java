package pe.nexosoftware.planilla360.asistencia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.nexosoftware.planilla360.asistencia.domain.excepcion.ReglaDeAsistenciaException;

class HorarioTests {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime CINCO_PM = LocalTime.of(17, 0);

    @Test
    void unHorarioValidoExponeSusDatos() {
        Horario horario = new Horario(OCHO, CINCO_PM, 10, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY));

        assertThat(horario.entrada()).isEqualTo(OCHO);
        assertThat(horario.salida()).isEqualTo(CINCO_PM);
        assertThat(horario.toleranciaMin()).isEqualTo(10);
        assertThat(horario.dias()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
    }

    @Test
    void lunesAViernesTrabajaDeLunesAViernes() {
        Horario horario = Horario.lunesAViernes(OCHO, CINCO_PM, 10);

        assertThat(horario.dias()).containsExactlyInAnyOrder(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        assertThat(horario.trabajaEl(DayOfWeek.FRIDAY)).isTrue();
        assertThat(horario.trabajaEl(DayOfWeek.SATURDAY)).isFalse();
        assertThat(horario.trabajaEl(DayOfWeek.SUNDAY)).isFalse();
    }

    @Test
    void elLimiteSinTardanzaEsLaEntradaMasLaTolerancia() {
        assertThat(Horario.lunesAViernes(OCHO, CINCO_PM, 10).limiteSinTardanza()).isEqualTo(LocalTime.of(8, 10));
        assertThat(Horario.lunesAViernes(OCHO, CINCO_PM, 0).limiteSinTardanza()).isEqualTo(OCHO);
    }

    @Test
    void laSalidaDebeSerPosteriorALaEntrada() {
        assertThatThrownBy(() -> Horario.lunesAViernes(CINCO_PM, OCHO, 10))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("salida");
        assertThatThrownBy(() -> Horario.lunesAViernes(OCHO, OCHO, 0))
                .isInstanceOf(ReglaDeAsistenciaException.class);
        assertThatCode(() -> Horario.lunesAViernes(OCHO, OCHO.plusMinutes(1), 0)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 61, 500})
    void laToleranciaDebeEstarEntre0Y60Minutos(int tolerancia) {
        assertThatThrownBy(() -> Horario.lunesAViernes(OCHO, CINCO_PM, tolerancia))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("tolerancia");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 59, 60})
    void laToleranciaEnLosExtremosEsValida(int tolerancia) {
        assertThatCode(() -> Horario.lunesAViernes(OCHO, CINCO_PM, tolerancia)).doesNotThrowAnyException();
    }

    @Test
    void laToleranciaDebeSerMenorQueLaJornada() {
        assertThatThrownBy(() -> Horario.lunesAViernes(OCHO, OCHO.plusMinutes(30), 30))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("jornada");
        assertThatCode(() -> Horario.lunesAViernes(OCHO, OCHO.plusMinutes(30), 29)).doesNotThrowAnyException();
    }

    @Test
    void debeTenerAlMenosUnDiaLaborable() {
        assertThatThrownBy(() -> new Horario(OCHO, CINCO_PM, 10, Set.of()))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("día");
        assertThatThrownBy(() -> new Horario(OCHO, CINCO_PM, 10, null))
                .isInstanceOf(ReglaDeAsistenciaException.class);
    }

    @Test
    void entradaYSalidaSonObligatorias() {
        assertThatThrownBy(() -> new Horario(null, CINCO_PM, 10, Set.of(DayOfWeek.MONDAY)))
                .isInstanceOf(ReglaDeAsistenciaException.class);
        assertThatThrownBy(() -> new Horario(OCHO, null, 10, Set.of(DayOfWeek.MONDAY)))
                .isInstanceOf(ReglaDeAsistenciaException.class);
    }

    @Test
    void losDiasSeCopianYNoSePuedenModificarDesdeFuera() {
        Set<DayOfWeek> dias = EnumSet.of(DayOfWeek.MONDAY);
        Horario horario = new Horario(OCHO, CINCO_PM, 10, dias);

        dias.add(DayOfWeek.TUESDAY);

        assertThat(horario.trabajaEl(DayOfWeek.TUESDAY)).isFalse();
        assertThatThrownBy(() -> horario.dias().add(DayOfWeek.FRIDAY))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
