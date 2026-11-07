package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.parametros;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.Gratificacion;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

@DisplayName("Gratificación")
class CalculadoraGratificacionTests {

    private static final Dinero BASICO = Dinero.de("3000");
    private static final Dinero ASIGNACION = Dinero.de("113");
    private static final List<Dinero> SIN_HORAS_EXTRA = List.of();

    private final CalculadoraGratificacion calculadora = new CalculadoraGratificacion();

    @ParameterizedTest(name = "mes {0} -> {1}")
    @CsvSource({
        "1, false", "2, false", "3, false", "4, false", "5, false", "6, false",
        "7, true", "8, false", "9, false", "10, false", "11, false", "12, true"
    })
    void sePagaEnJulioYDiciembre(int mes, boolean corresponde) {
        assertThat(calculadora.esMesDeGratificacion(Periodo.de(2026, mes))).isEqualTo(corresponde);
    }

    @Test
    void semestreCompletoPagaUnaRemuneracionMasLaBonificacionExtraordinaria() {
        Gratificacion g = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, 6, RegimenLaboral.GENERAL,
                referencia());
        assertThat(g.monto()).isEqualTo(Dinero.de("3113.00"));
        assertThat(g.bonificacionExtraordinaria()).isEqualTo(Dinero.de("280.17"));
        assertThat(g.total()).isEqualTo(Dinero.de("3393.17"));
    }

    @ParameterizedTest(name = "{0} meses completos -> gratificación {1}, bonificación {2}")
    @CsvSource({
        "0, 0.00, 0.00",
        "1, 518.83, 46.69",
        "3, 1556.50, 140.09",
        "4, 2075.33, 186.78",
        "5, 2594.17, 233.48",
        "6, 3113.00, 280.17"
    })
    void esProporcionalALosMesesCompletos(int meses, String monto, String bonificacion) {
        Gratificacion g = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, meses, RegimenLaboral.GENERAL,
                referencia());
        assertThat(g.monto()).isEqualTo(Dinero.de(monto));
        assertThat(g.bonificacionExtraordinaria()).isEqualTo(Dinero.de(bonificacion));
    }

    @Test
    void laPequenaEmpresaPagaLaMitad() {
        Gratificacion g = calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, 6,
                RegimenLaboral.PEQUENA_EMPRESA, referencia());
        assertThat(g.monto()).isEqualTo(Dinero.de("1556.50"));
        assertThat(g.bonificacionExtraordinaria()).isEqualTo(Dinero.de("140.09"));
    }

    @Test
    void laBonificacionUsaElPorcentajeVigente() {
        Gratificacion g = calculadora.calcular(BASICO, Dinero.CERO, SIN_HORAS_EXTRA, 6, RegimenLaboral.GENERAL,
                parametros().conPctBonificacionExtraordinaria("5").build());
        assertThat(g.bonificacionExtraordinaria()).isEqualTo(Dinero.de("150.00"));
    }

    @Test
    void incluyeElPromedioDeHorasExtraSiHuboEnTresMesesOMas() {
        List<Dinero> horasExtra = List.of(Dinero.de("300"), Dinero.de("300"), Dinero.de("300"), Dinero.CERO,
                Dinero.CERO, Dinero.CERO);
        assertThat(calculadora.remuneracionComputable(BASICO, Dinero.CERO, horasExtra))
                .isEqualTo(Dinero.de("3150.00"));
        Gratificacion g = calculadora.calcular(BASICO, Dinero.CERO, horasExtra, 6, RegimenLaboral.GENERAL,
                referencia());
        assertThat(g.monto()).isEqualTo(Dinero.de("3150.00"));
        assertThat(g.bonificacionExtraordinaria()).isEqualTo(Dinero.de("283.50"));
    }

    @Test
    void ignoraLasHorasExtraSiHuboEnMenosDeTresMeses() {
        List<Dinero> horasExtra = List.of(Dinero.de("500"), Dinero.CERO, Dinero.de("500"), Dinero.CERO);
        assertThat(calculadora.remuneracionComputable(BASICO, ASIGNACION, horasExtra))
                .isEqualTo(Dinero.de("3113.00"));
    }

    @Test
    void laRemuneracionComputableSumaBasicoAsignacionYHorasExtra() {
        List<Dinero> horasExtra = List.of(Dinero.de("100"), Dinero.de("200"), Dinero.de("300"), Dinero.de("400"),
                Dinero.de("500"), Dinero.de("600"));
        assertThat(calculadora.remuneracionComputable(BASICO, ASIGNACION, horasExtra))
                .isEqualTo(Dinero.de("3113").sumar(Dinero.de("350")));
    }

    @Test
    void rechazaMesesFueraDeRango() {
        assertThatThrownBy(() -> calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, -1,
                RegimenLaboral.GENERAL, referencia())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.calcular(BASICO, ASIGNACION, SIN_HORAS_EXTRA, 7,
                RegimenLaboral.GENERAL, referencia())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaMasDeSeisMesesDeHorasExtraOMontosNegativos() {
        List<Dinero> siete = List.of(Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO, Dinero.CERO,
                Dinero.CERO);
        assertThatThrownBy(() -> calculadora.remuneracionComputable(BASICO, ASIGNACION, siete))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.remuneracionComputable(BASICO, ASIGNACION, List.of(Dinero.de("-1"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elTotalSumaGratificacionYBonificacion() {
        assertThat(new Gratificacion(Dinero.de("100"), Dinero.de("9")).total()).isEqualTo(Dinero.de("109"));
    }
}
