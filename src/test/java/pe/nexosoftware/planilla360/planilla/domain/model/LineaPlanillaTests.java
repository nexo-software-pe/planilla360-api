package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Afp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;

@DisplayName("LineaPlanilla (boleta)")
class LineaPlanillaTests {

    private static final UUID EMPLEADO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static LineaPlanilla.Builder tipica() {
        return LineaPlanilla.de(EMPLEADO)
                .basico(Dinero.de("3000"))
                .asignacionFamiliar(Dinero.de("113"))
                .regimenPensionario(new Onp());
    }

    @Test
    void calculaTotalesYNetoDeUnMesNormal() {
        LineaPlanilla linea = tipica()
                .descuentoAsistencia(Dinero.de("6.25"))
                .otrosDescuentos(Dinero.de("100"))
                .build(referencia());

        assertThat(linea.remuneracionAsegurable()).isEqualTo(Dinero.de("3113.00"));
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.de("404.69"));
        assertThat(linea.aporteEssalud()).isEqualTo(Dinero.de("280.17"));
        assertThat(linea.totalIngresos()).isEqualTo(Dinero.de("3113.00"));
        assertThat(linea.totalDescuentos()).isEqualTo(Dinero.de("510.94"));
        assertThat(linea.neto()).isEqualTo(Dinero.de("2602.06"));
    }

    @Test
    void conAfpDesglosaAporteFondoPrimaYComision() {
        LineaPlanilla linea = tipica().regimenPensionario(new Afp("Prima", new BigDecimal("1.60")))
                .build(referencia());

        assertThat(linea.descuentoPension().aporte()).isEqualTo(Dinero.de("311.30"));
        assertThat(linea.descuentoPension().prima()).isEqualTo(Dinero.de("42.65"));
        assertThat(linea.descuentoPension().comision()).isEqualTo(Dinero.de("49.81"));
        assertThat(linea.neto()).isEqualTo(Dinero.de("2709.24"));
    }

    @Test
    void horasExtraYBonosEntranEnLaRemuneracionAsegurable() {
        LineaPlanilla linea = tipica().horasExtra(Dinero.de("200")).bonos(Dinero.de("100")).build(referencia());

        assertThat(linea.remuneracionAsegurable()).isEqualTo(Dinero.de("3413.00"));
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.de("443.69"));
        assertThat(linea.totalIngresos()).isEqualTo(Dinero.de("3413.00"));
    }

    @Test
    void laGratificacionSumaIngresosPeroNoPagaPension() {
        Gratificacion gratificacion = new Gratificacion(Dinero.de("3113"), Dinero.de("280.17"));
        LineaPlanilla linea = tipica().gratificacion(gratificacion).build(referencia());

        assertThat(linea.remuneracionAsegurable()).isEqualTo(Dinero.de("3113.00"));
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.de("404.69"));
        assertThat(linea.gratificacion()).isEqualTo(Dinero.de("3113.00"));
        assertThat(linea.bonificacionExtraordinaria()).isEqualTo(Dinero.de("280.17"));
        assertThat(linea.totalIngresos()).isEqualTo(Dinero.de("6506.17"));
        assertThat(linea.neto()).isEqualTo(Dinero.de("6101.48"));
    }

    @Test
    void sinGratificacionLosMontosSonCero() {
        LineaPlanilla linea = tipica().build(referencia());
        assertThat(linea.gratificacion()).isEqualTo(Dinero.CERO);
        assertThat(linea.bonificacionExtraordinaria()).isEqualTo(Dinero.CERO);
    }

    @Test
    void elNetoPuedeSerNegativoYLoDetectaLaValidacionDeCierre() {
        LineaPlanilla linea = tipica().otrosDescuentos(Dinero.de("5000")).build(referencia());
        assertThat(linea.neto().esNegativo()).isTrue();
    }

    @Test
    void laRetencionDeQuintaRestaDelNeto() {
        LineaPlanilla linea = tipica().retencion5ta(Dinero.de("30.33")).build(referencia());
        assertThat(linea.retencion5ta()).isEqualTo(Dinero.de("30.33"));
        assertThat(linea.totalDescuentos()).isEqualTo(Dinero.de("435.02"));
    }

    @Test
    void listaSoloLosConceptosConMontoParaLaBoletaOnp() {
        LineaPlanilla linea = tipica().descuentoAsistencia(Dinero.de("6.25")).build(referencia());

        assertThat(linea.conceptos())
                .extracting(Concepto::codigo, Concepto::tipo, Concepto::monto)
                .containsExactly(
                        tuple("BASICO", TipoConcepto.INGRESO, Dinero.de("3000")),
                        tuple("ASIGNACION_FAMILIAR", TipoConcepto.INGRESO, Dinero.de("113")),
                        tuple("ONP", TipoConcepto.DESCUENTO, Dinero.de("404.69")),
                        tuple("TARDANZAS_FALTAS", TipoConcepto.DESCUENTO, Dinero.de("6.25")),
                        tuple("ESSALUD", TipoConcepto.APORTE_EMPLEADOR, Dinero.de("280.17")));
    }

    @Test
    void laBoletaAfpTieneTresConceptosDePension() {
        LineaPlanilla linea = tipica().regimenPensionario(new Afp("Prima", new BigDecimal("1.60")))
                .build(referencia());

        assertThat(linea.conceptos()).extracting(Concepto::codigo)
                .containsExactly("BASICO", "ASIGNACION_FAMILIAR", "AFP_APORTE", "AFP_PRIMA", "AFP_COMISION",
                        "ESSALUD");
    }

    @Test
    void incluyeTodosLosConceptosCuandoHayMonto() {
        Gratificacion gratificacion = new Gratificacion(Dinero.de("100"), Dinero.de("9"));
        LineaPlanilla linea = tipica().horasExtra(Dinero.de("50")).bonos(Dinero.de("20"))
                .gratificacion(gratificacion).retencion5ta(Dinero.de("10")).descuentoAsistencia(Dinero.de("5"))
                .otrosDescuentos(Dinero.de("7")).build(referencia());

        assertThat(linea.conceptos()).extracting(Concepto::codigo)
                .containsExactly("BASICO", "ASIGNACION_FAMILIAR", "HORAS_EXTRA", "BONOS", "GRATIFICACION",
                        "BONIFICACION_EXTRAORDINARIA", "ONP", "RENTA_5TA", "TARDANZAS_FALTAS", "OTROS_DESCUENTOS",
                        "ESSALUD");
    }

    @Test
    void losConceptosNoSePuedenModificar() {
        LineaPlanilla linea = tipica().build(referencia());
        assertThatThrownBy(() -> linea.conceptos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void exigeEmpleadoBasicoYRegimenPensionario() {
        ParametrosLegales parametros = referencia();
        assertThatNullPointerException().isThrownBy(() -> LineaPlanilla.de(null).basico(Dinero.de("1"))
                .regimenPensionario(new Onp()).build(parametros));
        assertThatNullPointerException().isThrownBy(() -> LineaPlanilla.de(EMPLEADO)
                .regimenPensionario(new Onp()).build(parametros));
        assertThatNullPointerException().isThrownBy(() -> LineaPlanilla.de(EMPLEADO).basico(Dinero.de("1"))
                .build(parametros));
    }

    @Test
    void rechazaMontosNegativos() {
        ParametrosLegales parametros = referencia();
        Dinero negativo = Dinero.de("-1");
        assertThatThrownBy(() -> tipica().basico(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tipica().asignacionFamiliar(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tipica().horasExtra(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tipica().bonos(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tipica().retencion5ta(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tipica().descuentoAsistencia(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tipica().otrosDescuentos(negativo).build(parametros))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
