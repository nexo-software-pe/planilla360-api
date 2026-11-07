package pe.nexosoftware.planilla360.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Cada prefijo de contribuyente válido (10, 15, 17, 20) se acepta con su dígito verificador correcto. */
class RucPrefijosTests {

    @ParameterizedTest(name = "RUC {0} es válido")
    @ValueSource(strings = {"10458712366", "15458712368", "17458712361", "20458712361"})
    void aceptaTodosLosPrefijosDeContribuyente(String ruc) {
        assertThat(Ruc.esValido(ruc)).isTrue();
    }

    @Test
    void rechazaUnPrefijoNoAsignadoAunqueElDigitoVerificadorSeaCorrecto() {
        assertThat(Ruc.esValido("16458712364")).isFalse();
    }

    @Test
    void rechazaUnDigitoVerificadorIncorrectoEnCadaPrefijo() {
        assertThat(Ruc.esValido("10458712367")).isFalse();
        assertThat(Ruc.esValido("15458712369")).isFalse();
        assertThat(Ruc.esValido("17458712360")).isFalse();
    }

    @Test
    void elDniSeMuestraConSusOchoDigitos() {
        assertThat(new Dni("45871236")).hasToString("45871236");
    }
}
