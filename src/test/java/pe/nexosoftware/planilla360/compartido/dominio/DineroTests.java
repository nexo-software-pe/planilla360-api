package pe.nexosoftware.planilla360.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Dinero")
class DineroTests {

    @Test
    void siempreTieneDosDecimales() {
        assertThat(Dinero.de("10").monto()).isEqualTo(new BigDecimal("10.00"));
        assertThat(Dinero.de("10.5").monto().scale()).isEqualTo(2);
        assertThat(Dinero.de(7).monto()).isEqualTo(new BigDecimal("7.00"));
        assertThat(Dinero.de(new BigDecimal("3.1")).monto().scale()).isEqualTo(2);
    }

    @Test
    void montosIgualesSonIgualesAunqueSeEscribanDistinto() {
        assertThat(Dinero.de("10")).isEqualTo(Dinero.de("10.00")).hasSameHashCodeAs(Dinero.de("10.0"));
        assertThat(Dinero.de("10")).isNotEqualTo(Dinero.de("10.01"));
    }

    @ParameterizedTest(name = "{0} se redondea a {1}")
    @CsvSource({"2.344, 2.34", "2.345, 2.35", "2.346, 2.35", "-2.344, -2.34", "-2.345, -2.35", "0.004, 0.00"})
    void redondeaMitadHaciaArriba(String entrada, String esperado) {
        assertThat(Dinero.de(entrada)).isEqualTo(Dinero.de(esperado));
    }

    @Test
    void rechazaNulos() {
        assertThatNullPointerException().isThrownBy(() -> Dinero.de((String) null));
        assertThatNullPointerException().isThrownBy(() -> Dinero.de((BigDecimal) null));
        assertThatNullPointerException().isThrownBy(() -> new Dinero(null));
    }

    @Test
    void rechazaTextoQueNoEsNumero() {
        assertThatThrownBy(() -> Dinero.de("abc")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void ceroEsCero() {
        assertThat(Dinero.CERO).isEqualTo(Dinero.de("0.00"));
        assertThat(Dinero.CERO.esCero()).isTrue();
        assertThat(Dinero.de("0.01").esCero()).isFalse();
        assertThat(Dinero.de("-0.01").esCero()).isFalse();
    }

    @Test
    void sumaYResta() {
        assertThat(Dinero.de("10.10").sumar(Dinero.de("0.20"))).isEqualTo(Dinero.de("10.30"));
        assertThat(Dinero.de("10.10").restar(Dinero.de("0.20"))).isEqualTo(Dinero.de("9.90"));
        assertThat(Dinero.de("1").restar(Dinero.de("3"))).isEqualTo(Dinero.de("-2"));
    }

    @Test
    void sumaDeUnaColeccion() {
        assertThat(Dinero.suma(List.of(Dinero.de("1.10"), Dinero.de("2.20"), Dinero.de("3.30"))))
                .isEqualTo(Dinero.de("6.60"));
        assertThat(Dinero.suma(List.of())).isEqualTo(Dinero.CERO);
    }

    @Test
    void multiplicaYRedondea() {
        assertThat(Dinero.de("100").multiplicar(new BigDecimal("0.135"))).isEqualTo(Dinero.de("13.50"));
        assertThat(Dinero.de("10.01").multiplicar(new BigDecimal("0.5"))).isEqualTo(Dinero.de("5.01"));
        assertThat(Dinero.de("10").multiplicar(BigDecimal.ZERO)).isEqualTo(Dinero.CERO);
    }

    @ParameterizedTest(name = "{1} % de {0} = {2}")
    @CsvSource({"1500, 13, 195.00", "3022.80, 9, 272.05", "1130, 10, 113.00", "100, 1.37, 1.37", "0, 13, 0.00"})
    void calculaPorcentajes(String base, String porcentaje, String esperado) {
        assertThat(Dinero.de(base).porcentaje(new BigDecimal(porcentaje))).isEqualTo(Dinero.de(esperado));
    }

    @ParameterizedTest(name = "{0} x {1}/{2} = {3}")
    @CsvSource({"100, 5, 6, 83.33", "100, 1, 3, 33.33", "100, 2, 3, 66.67", "3000, 4, 6, 2000.00", "50, 0, 6, 0.00"})
    void calculaProporcionesConUnSoloRedondeo(String base, long numerador, long denominador, String esperado) {
        assertThat(Dinero.de(base).proporcional(numerador, denominador)).isEqualTo(Dinero.de(esperado));
    }

    @Test
    void laProporcionNoAcumulaErroresDeRedondeo() {
        // 1000 x 3/3 debe dar 1000 exacto: se multiplica antes de dividir y se redondea una sola vez.
        assertThat(Dinero.de("1000").proporcional(3, 3)).isEqualTo(Dinero.de("1000"));
    }

    @Test
    void laProporcionRechazaDenominadorCeroONegativo() {
        assertThatThrownBy(() -> Dinero.de("10").proporcional(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dinero.de("10").proporcional(1, -2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparaMaximoYMinimo() {
        Dinero cinco = Dinero.de("5");
        Dinero diez = Dinero.de("10");
        assertThat(cinco.compareTo(diez)).isNegative();
        assertThat(diez.compareTo(cinco)).isPositive();
        assertThat(diez.compareTo(Dinero.de("10.00"))).isZero();
        assertThat(cinco.max(diez)).isEqualTo(diez);
        assertThat(diez.max(cinco)).isEqualTo(diez);
        assertThat(cinco.min(diez)).isEqualTo(cinco);
        assertThat(diez.min(cinco)).isEqualTo(cinco);
    }

    @Test
    void detectaNegativos() {
        assertThat(Dinero.de("-0.01").esNegativo()).isTrue();
        assertThat(Dinero.CERO.esNegativo()).isFalse();
        assertThat(Dinero.de("0.01").esNegativo()).isFalse();
    }

    @Test
    void esInmutable() {
        Dinero original = Dinero.de("10");
        original.sumar(Dinero.de("5"));
        assertThat(original).isEqualTo(Dinero.de("10"));
    }

    @Test
    void sePresentaConDosDecimales() {
        assertThat(Dinero.de("3022.8")).hasToString("3022.80");
    }
}
