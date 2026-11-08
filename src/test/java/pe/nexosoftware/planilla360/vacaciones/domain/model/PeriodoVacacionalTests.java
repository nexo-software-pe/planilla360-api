package pe.nexosoftware.planilla360.vacaciones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.nexosoftware.planilla360.vacaciones.domain.excepcion.ReglaDeVacacionesException;

class PeriodoVacacionalTests {

    private static final LocalDate DESDE = LocalDate.of(2025, 3, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 2, 28);

    private static PeriodoVacacional periodo(int ganados, int gozados, int vendidos) {
        return new PeriodoVacacional(UUID.randomUUID(), DESDE, HASTA, ganados, gozados, vendidos);
    }

    @Test
    void laPoliticaGeneralDa30DiasYPermiteVenderHasta15() {
        PoliticaVacaciones general = PoliticaVacaciones.general();

        assertThat(general.diasPorAnio()).isEqualTo(30);
        assertThat(general.maximoDiasVendibles()).isEqualTo(15);
        assertThat(general).isInstanceOf(PoliticaVacaciones.General.class);
    }

    @Test
    void laPoliticaDePequenaEmpresaDa15DiasYNoPermiteVender() {
        PoliticaVacaciones pequena = PoliticaVacaciones.pequenaEmpresa();

        assertThat(pequena.diasPorAnio()).isEqualTo(15);
        assertThat(pequena.maximoDiasVendibles()).isZero();
        assertThat(pequena).isInstanceOf(PoliticaVacaciones.PequenaEmpresa.class);
    }

    @Test
    void elSaldoEsGanadosMenosGozadosMenosVendidos() {
        assertThat(periodo(30, 0, 0).saldo()).isEqualTo(30);
        assertThat(periodo(30, 10, 5).saldo()).isEqualTo(15);
        assertThat(periodo(30, 20, 10).saldo()).isZero();
    }

    @Test
    void exponeSusDatos() {
        UUID id = UUID.randomUUID();
        PeriodoVacacional p = new PeriodoVacacional(id, DESDE, HASTA, 30, 7, 3);

        assertThat(p.id()).isEqualTo(id);
        assertThat(p.desde()).isEqualTo(DESDE);
        assertThat(p.hasta()).isEqualTo(HASTA);
        assertThat(p.diasGanados()).isEqualTo(30);
        assertThat(p.diasGozados()).isEqualTo(7);
        assertThat(p.diasVendidos()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"-1, 0, 0", "30, -1, 0", "30, 0, -1", "30, 20, 11", "10, 11, 0", "10, 0, 11"})
    void noAceptaDiasNegativosNiConsumirMasDeLoGanado(int ganados, int gozados, int vendidos) {
        assertThatThrownBy(() -> periodo(ganados, gozados, vendidos)).isInstanceOf(ReglaDeVacacionesException.class);
    }

    @Test
    void aceptaConsumirExactamenteLoGanado() {
        assertThatCode(() -> periodo(30, 20, 10)).doesNotThrowAnyException();
        assertThatCode(() -> periodo(0, 0, 0)).doesNotThrowAnyException();
    }

    @Test
    void registrarGoceDescuentaDelSaldo() {
        PeriodoVacacional p = periodo(30, 0, 0);

        p.registrarGoce(10);
        p.registrarGoce(20);

        assertThat(p.diasGozados()).isEqualTo(30);
        assertThat(p.saldo()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3, 31})
    void registrarGoceExigeDiasPositivosYNoMasQueElSaldo(int dias) {
        PeriodoVacacional p = periodo(30, 0, 0);

        assertThatThrownBy(() -> p.registrarGoce(dias)).isInstanceOf(ReglaDeVacacionesException.class);
        assertThat(p.diasGozados()).isZero();
    }

    @Test
    void venderDescuentaDelSaldoEnElRegimenGeneral() {
        PeriodoVacacional p = periodo(30, 0, 0);

        p.vender(10, PoliticaVacaciones.general());
        p.vender(5, PoliticaVacaciones.general());

        assertThat(p.diasVendidos()).isEqualTo(15);
        assertThat(p.saldo()).isEqualTo(15);
    }

    @Test
    void noSePuedeVenderMasDe15DiasEnTotal() {
        PeriodoVacacional p = periodo(30, 0, 10);

        assertThatThrownBy(() -> p.vender(6, PoliticaVacaciones.general()))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("15");
        assertThatCode(() -> p.vender(5, PoliticaVacaciones.general())).doesNotThrowAnyException();
    }

    @Test
    void noSePuedeVenderMasDiasQueElSaldo() {
        PeriodoVacacional p = periodo(30, 20, 0);

        assertThatThrownBy(() -> p.vender(11, PoliticaVacaciones.general()))
                .isInstanceOf(ReglaDeVacacionesException.class);
        assertThat(p.diasVendidos()).isZero();
    }

    @Test
    void laPequenaEmpresaNoPuedeVender() {
        PeriodoVacacional p = periodo(15, 0, 0);

        assertThatThrownBy(() -> p.vender(1, PoliticaVacaciones.pequenaEmpresa()))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("régimen");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void venderExigeDiasPositivos(int dias) {
        assertThatThrownBy(() -> periodo(30, 0, 0).vender(dias, PoliticaVacaciones.general()))
                .isInstanceOf(ReglaDeVacacionesException.class);
    }
}
