package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.parametros;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** UIT de referencia 5 350: deducción 7 UIT = 37 450; tramos en 26 750 / 107 000 / 187 250 / 240 750. */
@DisplayName("Renta de 5.ª categoría")
class CalculadoraRenta5taTests {

    private final CalculadoraRenta5ta calculadora = new CalculadoraRenta5ta();

    @ParameterizedTest(name = "renta bruta anual {0} -> impuesto {1}")
    @CsvSource({
        "0, 0.00",
        "30000, 0.00",
        "37450, 0.00",
        "37451, 0.08",
        "60000, 1804.00",
        "64200, 2140.00",
        "64201, 2140.14",
        "100000, 7152.00",
        "144450, 13375.00",
        "200000, 22818.50",
        "400000, 74257.50"
    })
    void aplicaLaEscalaProgresivaDespuesDeDeducir7Uit(String rentaBruta, String esperado) {
        assertThat(calculadora.impuestoAnual(Dinero.de(rentaBruta), referencia())).isEqualTo(Dinero.de(esperado));
    }

    @Test
    void laDeduccionYLosTramosDependenDeLaUitVigente() {
        // UIT 5 000: deducción 35 000; neta 15 000 -> 8 % de 15 000.
        assertThat(calculadora.impuestoAnual(Dinero.de("50000"), parametros().conUit("5000").build()))
                .isEqualTo(Dinero.de("1200.00"));
    }

    @Test
    void laRetencionMensualEsElImpuestoAnualProyectadoEntreDoce() {
        // 3 000 x 12 + 6 000 de gratificaciones = 42 000; neta 4 550 -> 364 anual -> 30,33 al mes.
        assertThat(calculadora.retencionMensual(Dinero.de("3000"), 12, Dinero.de("6000"), Dinero.CERO,
                referencia())).isEqualTo(Dinero.de("30.33"));
    }

    @Test
    void laProyeccionSumaLoYaPercibidoMasLoQueFalta() {
        // 30 000 percibidos + 5 000 x 6 meses + 5 000 de gratificación = 65 000; neta 27 550 -> 2 252 -> 187,67.
        assertThat(calculadora.retencionMensual(Dinero.de("5000"), 6, Dinero.de("5000"), Dinero.de("30000"),
                referencia())).isEqualTo(Dinero.de("187.67"));
    }

    @Test
    void quienNoSuperaLas7UitNoTieneRetencion() {
        assertThat(calculadora.retencionMensual(Dinero.de("2000"), 12, Dinero.de("4000"), Dinero.CERO,
                referencia())).isEqualTo(Dinero.CERO);
    }

    @Test
    void aceptaDesdeUnMesHastaDoceMeses() {
        assertThat(calculadora.retencionMensual(Dinero.de("7000"), 1, Dinero.CERO, Dinero.CERO, referencia()))
                .isEqualTo(Dinero.CERO);
        assertThat(calculadora.retencionMensual(Dinero.de("7000"), 12, Dinero.CERO, Dinero.CERO, referencia()))
                .isEqualTo(Dinero.de("409.33"));
    }

    @Test
    void laRemuneracionNegativaSeRechazaAunqueLosOtrosMontosCompensen() {
        assertThatThrownBy(() -> calculadora.retencionMensual(Dinero.de("-1"), 12, Dinero.CERO,
                Dinero.de("1000"), referencia())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remuneración mensual");
    }

    @Test
    void rechazaDatosInvalidos() {
        Dinero uno = Dinero.de("1");
        assertThatThrownBy(() -> calculadora.retencionMensual(uno, 0, uno, uno, referencia()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.retencionMensual(uno, 13, uno, uno, referencia()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.retencionMensual(Dinero.de("-1"), 12, uno, uno, referencia()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.retencionMensual(uno, 12, Dinero.de("-1"), uno, referencia()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.retencionMensual(uno, 12, uno, Dinero.de("-1"), referencia()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.impuestoAnual(Dinero.de("-1"), referencia()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
