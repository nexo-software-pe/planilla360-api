package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Afp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;

@DisplayName("Línea de planilla de CTS")
class LineaPlanillaCtsTests {

    private static final UUID EMPLEADO = new UUID(0, 7);

    @Test
    void elDepositoDeCtsEsElUnicoIngresoDeLaLinea() {
        LineaPlanilla linea = LineaPlanilla.de(EMPLEADO).basico(Dinero.CERO).cts(Dinero.de("1815.92"))
                .regimenPensionario(new Onp()).build(referencia());

        assertThat(linea.cts()).isEqualTo(Dinero.de("1815.92"));
        assertThat(linea.totalIngresos()).isEqualTo(Dinero.de("1815.92"));
        assertThat(linea.totalDescuentos()).isEqualTo(Dinero.CERO);
        assertThat(linea.aporteEssalud()).isEqualTo(Dinero.CERO);
        assertThat(linea.neto()).isEqualTo(Dinero.de("1815.92"));
    }

    @Test
    void laCtsNoEsRemuneracionAsegurableNiPagaPension() {
        LineaPlanilla linea = LineaPlanilla.de(EMPLEADO).basico(Dinero.de("3000")).cts(Dinero.de("1000"))
                .regimenPensionario(new Afp("Prima", new java.math.BigDecimal("1.60"))).build(referencia());

        assertThat(linea.remuneracionAsegurable()).isEqualTo(Dinero.de("3000.00"));
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.de("389.10"));
        assertThat(linea.totalIngresos()).isEqualTo(Dinero.de("4000.00"));
    }

    @Test
    void sinCtsElMontoEsCero() {
        LineaPlanilla linea = LineaPlanilla.de(EMPLEADO).basico(Dinero.de("3000")).regimenPensionario(new Onp())
                .build(referencia());

        assertThat(linea.cts()).isEqualTo(Dinero.CERO);
    }

    @Test
    void laCtsApareceEnLaBoletaComoIngresoDespuesDeLaBonificacion() {
        LineaPlanilla linea = LineaPlanilla.de(EMPLEADO).basico(Dinero.CERO).cts(Dinero.de("1815.92"))
                .regimenPensionario(new Onp()).build(referencia());

        assertThat(linea.conceptos()).containsExactly(new Concepto("CTS",
                "Compensación por tiempo de servicios (depósito)", TipoConcepto.INGRESO, Dinero.de("1815.92")));
    }

    @Test
    void laCtsNoPuedeSerNegativa() {
        LineaPlanilla.Builder builder = LineaPlanilla.de(EMPLEADO).basico(Dinero.CERO).cts(Dinero.de("-1"))
                .regimenPensionario(new Onp());

        assertThatThrownBy(() -> builder.build(referencia()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("La CTS no puede ser negativo");
    }
}
