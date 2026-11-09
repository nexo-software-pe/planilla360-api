package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

@DisplayName("Calendario laboral")
class CalendarioLaboralTests {

    private static final Predicate<LocalDate> SIN_FERIADOS = fecha -> false;

    private final CalendarioLaboral calendario = new CalendarioLaboral();

    @Test
    void siElMesTerminaEnDiaDeSemanaEsElUltimoDiaHabil() {
        assertThat(calendario.ultimoDiaHabil(Periodo.de(2026, 11), SIN_FERIADOS)).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(calendario.ultimoDiaHabil(Periodo.de(2026, 3), SIN_FERIADOS)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void siElMesTerminaEnSabadoElUltimoDiaHabilEsElViernes() {
        assertThat(calendario.ultimoDiaHabil(Periodo.de(2026, 10), SIN_FERIADOS)).isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    void siElMesTerminaEnDomingoElUltimoDiaHabilEsElViernes() {
        assertThat(calendario.ultimoDiaHabil(Periodo.de(2026, 5), SIN_FERIADOS)).isEqualTo(LocalDate.of(2026, 5, 29));
    }

    @Test
    void unFeriadoNoEsDiaHabil() {
        Set<LocalDate> feriados = Set.of(LocalDate.of(2026, 12, 31));

        assertThat(calendario.ultimoDiaHabil(Periodo.de(2026, 12), feriados::contains))
                .isEqualTo(LocalDate.of(2026, 12, 30));
    }

    @Test
    void sePuedenSaltarFinDeSemanaYFeriadosJuntos() {
        Set<LocalDate> feriados = Set.of(LocalDate.of(2026, 5, 29));

        assertThat(calendario.ultimoDiaHabil(Periodo.de(2026, 5), feriados::contains))
                .isEqualTo(LocalDate.of(2026, 5, 28));
    }

    @Test
    void sinNingunDiaHabilEsUnError() {
        assertThatThrownBy(() -> calendario.ultimoDiaHabil(Periodo.de(2026, 5), fecha -> true))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("2026-05");
    }

    @Test
    void reconoceElUltimoDiaHabilDeUnaFecha() {
        assertThat(calendario.esUltimoDiaHabil(LocalDate.of(2026, 10, 30), SIN_FERIADOS)).isTrue();
        assertThat(calendario.esUltimoDiaHabil(LocalDate.of(2026, 10, 29), SIN_FERIADOS)).isFalse();
        assertThat(calendario.esUltimoDiaHabil(LocalDate.of(2026, 10, 31), SIN_FERIADOS)).isFalse();
    }
}
