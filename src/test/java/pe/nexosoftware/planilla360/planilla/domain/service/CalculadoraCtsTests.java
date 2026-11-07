package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DepositoCts;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

@DisplayName("CTS")
class CalculadoraCtsTests {

    private static final Dinero BASICO = Dinero.de("3000");
    private static final Dinero ASIGNACION = Dinero.de("113");
    private static final Dinero ULTIMA_GRATIFICACION = Dinero.de("3113");
    private static final List<Dinero> SIN_HORAS_EXTRA = List.of();

    private final CalculadoraCts calculadora = new CalculadoraCts(new CalculadoraGratificacion());

    @ParameterizedTest(name = "mes {0} -> {1}")
    @CsvSource({
        "1, false", "2, false", "3, false", "4, false", "5, true", "6, false",
        "7, false", "8, false", "9, false", "10, false", "11, true", "12, false"
    })
    void sedepositaEnMayoYNoviembre(int mes, boolean corresponde) {
        assertThat(calculadora.esMesDeDeposito(Periodo.de(2026, mes))).isEqualTo(corresponde);
    }

    @Test
    void laRemuneracionComputableIncluyeUnSextoDeLaUltimaGratificacion() {
        DepositoCts cts = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, ULTIMA_GRATIFICACION, 6, 0,
                RegimenLaboral.GENERAL);
        assertThat(cts.remuneracionComputable()).isEqualTo(Dinero.de("3631.83"));
    }

    @Test
    void semestreCompletoEsMediaRemuneracionComputable() {
        DepositoCts cts = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, ULTIMA_GRATIFICACION, 6, 0,
                RegimenLaboral.GENERAL);
        assertThat(cts.monto()).isEqualTo(Dinero.de("1815.92"));
    }

    @ParameterizedTest(name = "{0} meses y {1} días -> {2}")
    @CsvSource({
        "0, 0, 0.00",
        "0, 10, 100.88",
        "1, 0, 302.65",
        "3, 15, 1059.28",
        "5, 29, 1805.83",
        "6, 0, 1815.92"
    })
    void sumaMesesYDiasCompletos(int meses, int dias, String esperado) {
        DepositoCts cts = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, ULTIMA_GRATIFICACION, meses,
                dias, RegimenLaboral.GENERAL);
        assertThat(cts.monto()).isEqualTo(Dinero.de(esperado));
    }

    @Test
    void laPequenaEmpresaDepositaLaMitad() {
        DepositoCts cts = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, ULTIMA_GRATIFICACION, 6, 0,
                RegimenLaboral.PEQUENA_EMPRESA);
        assertThat(cts.monto()).isEqualTo(Dinero.de("907.96"));
    }

    @Test
    void incluyeElPromedioDeHorasExtraDelSemestre() {
        List<Dinero> horasExtra = List.of(Dinero.de("300"), Dinero.de("300"), Dinero.de("300"), Dinero.CERO,
                Dinero.CERO, Dinero.CERO);
        DepositoCts cts = calculadora.calcular(BASICO, Dinero.CERO, horasExtra, Dinero.de("3150"), 6, 0,
                RegimenLaboral.GENERAL);
        assertThat(cts.remuneracionComputable()).isEqualTo(Dinero.de("3675.00"));
        assertThat(cts.monto()).isEqualTo(Dinero.de("1837.50"));
    }

    @Test
    void sinGratificacionPreviaSoloCuentaElBasicoYLaAsignacion() {
        DepositoCts cts = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, Dinero.CERO, 6, 0,
                RegimenLaboral.GENERAL);
        assertThat(cts.remuneracionComputable()).isEqualTo(Dinero.de("3113.00"));
        assertThat(cts.monto()).isEqualTo(Dinero.de("1556.50"));
    }

    @Test
    void rechazaMesesODiasFueraDeRango() {
        assertThatThrownBy(() -> calcular(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calcular(7, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calcular(0, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calcular(0, 30)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unSemestreNoPuedeTenerMasDeSeisMesesEnTotal() {
        assertThatThrownBy(() -> calcular(6, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(calcular(5, 29).monto()).isEqualTo(Dinero.de("1805.83"));
    }

    @Test
    void rechazaUnaGratificacionNegativa() {
        assertThatThrownBy(() -> calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, Dinero.de("-1"), 6, 0,
                RegimenLaboral.GENERAL)).isInstanceOf(IllegalArgumentException.class);
    }

    private DepositoCts calcular(int meses, int dias) {
        return calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, ULTIMA_GRATIFICACION, meses, dias,
                RegimenLaboral.GENERAL);
    }
}
