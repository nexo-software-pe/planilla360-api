package pe.nexosoftware.planilla360.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RucYDniTests {

    @ParameterizedTest
    @ValueSource(strings = {"20601234565", "20602345671", "20603456786", "20604567891"})
    void rucConDigitoVerificadorCorrectoEsValido(String ruc) {
        assertThat(Ruc.esValido(ruc)).isTrue();
        assertThat(new Ruc(ruc)).hasToString(ruc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20601234566", "2060123456", "206012345650", "abcdefghijk", "30601234565", ""})
    void rucInvalidoSeRechaza(String ruc) {
        assertThat(Ruc.esValido(ruc)).isFalse();
        assertThatThrownBy(() -> new Ruc(ruc)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rucNuloNoEsValido() {
        assertThat(Ruc.esValido(null)).isFalse();
    }

    @Test
    void dniTieneOchoDigitos() {
        assertThat(Dni.esValido("45871236")).isTrue();
        assertThat(Dni.esValido("4587123")).isFalse();
        assertThat(Dni.esValido("4587123A")).isFalse();
        assertThat(Dni.esValido(null)).isFalse();
        assertThatThrownBy(() -> new Dni("123")).isInstanceOf(IllegalArgumentException.class);
    }
}
