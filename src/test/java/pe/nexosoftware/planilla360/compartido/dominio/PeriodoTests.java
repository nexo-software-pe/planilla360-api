package pe.nexosoftware.planilla360.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Periodo")
class PeriodoTests {

    @Test
    void seCreaConAnioYMes() {
        Periodo periodo = Periodo.de(2026, 7);
        assertThat(periodo.anio()).isEqualTo(2026);
        assertThat(periodo.mes()).isEqualTo(7);
    }

    @Test
    void seCreaDesdeTextoIso() {
        assertThat(Periodo.parse("2026-07")).isEqualTo(Periodo.de(2026, 7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13", "2026-00", "julio", "2026/07", "", "26-07"})
    void rechazaTextosInvalidos(String texto) {
        assertThatThrownBy(() -> Periodo.parse(texto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaMesFueraDeRango() {
        assertThatThrownBy(() -> Periodo.de(2026, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Periodo.de(2026, 13)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaNulos() {
        assertThatNullPointerException().isThrownBy(() -> Periodo.parse(null));
        assertThatNullPointerException().isThrownBy(() -> Periodo.de((LocalDate) null));
        assertThatNullPointerException().isThrownBy(() -> new Periodo(null));
    }

    @Test
    void seCreaDesdeUnaFecha() {
        assertThat(Periodo.de(LocalDate.of(2026, 11, 7))).isEqualTo(Periodo.de(2026, 11));
    }

    @ParameterizedTest(name = "{0}-{1} tiene {2} días")
    @CsvSource({"2026, 1, 31", "2026, 2, 28", "2028, 2, 29", "2026, 4, 30", "2026, 12, 31"})
    void conoceSusDias(int anio, int mes, int dias) {
        Periodo periodo = Periodo.de(anio, mes);
        assertThat(periodo.diasDelMes()).isEqualTo(dias);
        assertThat(periodo.inicio()).isEqualTo(LocalDate.of(anio, mes, 1));
        assertThat(periodo.fin()).isEqualTo(LocalDate.of(anio, mes, dias));
    }

    @Test
    void navegaEntreMeses() {
        assertThat(Periodo.de(2026, 1).anterior()).isEqualTo(Periodo.de(2025, 12));
        assertThat(Periodo.de(2026, 12).siguiente()).isEqualTo(Periodo.de(2027, 1));
        assertThat(Periodo.de(2026, 6).anterior()).isEqualTo(Periodo.de(2026, 5));
        assertThat(Periodo.de(2026, 6).siguiente()).isEqualTo(Periodo.de(2026, 7));
    }

    @Test
    void sabeSiContieneUnaFecha() {
        Periodo julio = Periodo.de(2026, 7);
        assertThat(julio.contiene(LocalDate.of(2026, 7, 1))).isTrue();
        assertThat(julio.contiene(LocalDate.of(2026, 7, 31))).isTrue();
        assertThat(julio.contiene(LocalDate.of(2026, 6, 30))).isFalse();
        assertThat(julio.contiene(LocalDate.of(2026, 8, 1))).isFalse();
        assertThat(julio.contiene(LocalDate.of(2025, 7, 15))).isFalse();
    }

    @Test
    void sePuedeOrdenar() {
        assertThat(Periodo.de(2026, 6)).isLessThan(Periodo.de(2026, 7));
        assertThat(Periodo.de(2025, 12)).isLessThan(Periodo.de(2026, 1));
        assertThat(Periodo.de(2026, 7)).isEqualByComparingTo(Periodo.de(2026, 7));
        assertThat(Periodo.de(2027, 1)).isGreaterThan(Periodo.de(2026, 12));
    }

    @Test
    void sePresentaComoAnioGuionMes() {
        assertThat(Periodo.de(2026, 3)).hasToString("2026-03");
    }
}
