package pe.nexosoftware.planilla360.planilla.domain.regimen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.parametros;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

@DisplayName("Régimen pensionario")
class RegimenPensionarioTests {

    @Nested
    @DisplayName("ONP")
    class OnpTests {

        private final RegimenPensionario onp = new Onp();

        @ParameterizedTest(name = "13 % de {0} = {1}")
        @CsvSource({"0, 0.00", "1130, 146.90", "1500, 195.00", "2500, 325.00", "3022.80, 392.96"})
        void descuentaElTrecePorCiento(String base, String esperado) {
            DescuentoPension d = onp.calcular(Dinero.de(base), referencia());
            assertThat(d.total()).isEqualTo(Dinero.de(esperado));
            assertThat(d.aporte()).isEqualTo(Dinero.de(esperado));
            assertThat(d.prima()).isEqualTo(Dinero.CERO);
            assertThat(d.comision()).isEqualTo(Dinero.CERO);
        }

        @Test
        void usaLaTasaVigenteDeLosParametros() {
            DescuentoPension d = onp.calcular(Dinero.de("1000"), parametros().conPctOnp("11").build());
            assertThat(d.total()).isEqualTo(Dinero.de("110.00"));
        }

        @Test
        void sePresentaComoOnp() {
            assertThat(onp.nombre()).isEqualTo("ONP");
        }

        @Test
        void rechazaUnaRemuneracionNegativa() {
            assertThatThrownBy(() -> onp.calcular(Dinero.de("-1"), referencia()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("AFP")
    class AfpTests {

        @ParameterizedTest(name = "AFP {1} % sobre {0}: aporte {2}, prima {3}, comisión {4}, total {5}")
        @CsvSource({
            "3000, 1.60, 300.00, 41.10, 48.00, 389.10",
            "3000, 1.25, 300.00, 41.10, 37.50, 378.60",
            "1130, 1.60, 113.00, 15.48, 18.08, 146.56",
            "0, 1.60, 0.00, 0.00, 0.00, 0.00"
        })
        void sumaAporteFondoPrimaYComision(String base, String comision, String aporte, String prima,
                String comisionMonto, String total) {
            Afp afp = new Afp("Prima", new BigDecimal(comision));
            DescuentoPension d = afp.calcular(Dinero.de(base), referencia());
            assertThat(d.aporte()).isEqualTo(Dinero.de(aporte));
            assertThat(d.prima()).isEqualTo(Dinero.de(prima));
            assertThat(d.comision()).isEqualTo(Dinero.de(comisionMonto));
            assertThat(d.total()).isEqualTo(Dinero.de(total));
        }

        @Test
        void usaLasTasasVigentesDeLosParametros() {
            Afp afp = new Afp("Integra", new BigDecimal("2"));
            DescuentoPension d = afp.calcular(Dinero.de("1000"),
                    parametros().conPctAfpAporte("8").conPctAfpPrima("1").build());
            assertThat(d.aporte()).isEqualTo(Dinero.de("80.00"));
            assertThat(d.prima()).isEqualTo(Dinero.de("10.00"));
            assertThat(d.comision()).isEqualTo(Dinero.de("20.00"));
        }

        @Test
        void sePresentaConElNombreDeLaAdministradora() {
            assertThat(new Afp("Prima", BigDecimal.ONE).nombre()).isEqualTo("AFP Prima");
        }

        @Test
        void rechazaUnaRemuneracionNegativa() {
            assertThatThrownBy(() -> new Afp("Prima", BigDecimal.ONE).calcular(Dinero.de("-1"), referencia()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"-0.1", "100.1"})
        void rechazaComisionesFueraDeRango(String comision) {
            assertThatThrownBy(() -> new Afp("Prima", new BigDecimal(comision)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource(value = {"''", "'   '"})
        void rechazaAdministradoraSinNombre(String nombre) {
            assertThatThrownBy(() -> new Afp(nombre, BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Total del descuento")
    class TotalTests {

        @Test
        void sumaLosTresComponentes() {
            DescuentoPension d = new DescuentoPension(Dinero.de("1.10"), Dinero.de("2.20"), Dinero.de("3.30"));
            assertThat(d.total()).isEqualTo(Dinero.de("6.60"));
        }
    }
}
