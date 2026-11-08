package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosMensualesTrabajador;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

@DisplayName("Cálculo mensual de un trabajador")
class CalculadoraLineaMensualTests {

    private static final UUID EMPLEADO = new UUID(0, 9);
    private static final BigDecimal S1 = new BigDecimal("25");
    private static final BigDecimal S2 = new BigDecimal("35");
    private static final Hijo HIJO_MENOR = new Hijo(LocalDate.of(2015, 6, 1), false);

    private final CalculadoraLineaMensual calculadora = new CalculadoraLineaMensual();

    /** Trabajador típico: básico 3 000, ONP, un hijo menor, mes completo de marzo y sin novedades. */
    private static DatosBuilder datos() {
        return new DatosBuilder();
    }

    private LineaPlanilla calcular(DatosMensualesTrabajador datos, int mes) {
        return calculadora.calcular(datos, Periodo.de(2026, mes), referencia(), S1, S2);
    }

    @Test
    void unMesCompletoConTodosLosConceptos() {
        DatosMensualesTrabajador datos = datos().horasExtra("2").percibido("6226").build();

        LineaPlanilla linea = calcular(datos, 3);

        assertThat(linea.empleadoId()).isEqualTo(EMPLEADO);
        assertThat(linea.basico()).isEqualTo(Dinero.de("3000.00"));
        assertThat(linea.asignacionFamiliar()).isEqualTo(Dinero.de("113.00"));
        assertThat(linea.horasExtra()).isEqualTo(Dinero.de("31.25"));
        assertThat(linea.remuneracionAsegurable()).isEqualTo(Dinero.de("3144.25"));
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.de("408.75"));
        assertThat(linea.retencion5ta()).isEqualTo(Dinero.de("44.62"));
        assertThat(linea.descuentoAsistencia()).isEqualTo(Dinero.CERO);
        assertThat(linea.aporteEssalud()).isEqualTo(Dinero.de("282.98"));
        assertThat(linea.gratificacion()).isEqualTo(Dinero.CERO);
        assertThat(linea.cts()).isEqualTo(Dinero.CERO);
        assertThat(linea.neto()).isEqualTo(Dinero.de("2690.88"));
    }

    @Test
    void quienIngresoAMitadDelMesCobraProporcionalALosDiasLaborados() {
        LineaPlanilla linea = calcular(datos().dias(16).build(), 3);

        assertThat(linea.basico()).isEqualTo(Dinero.de("1548.39"));
    }

    @Test
    void laAsignacionFamiliarSeCompletaAunqueElMesNoSeaCompleto() {
        LineaPlanilla linea = calcular(datos().dias(16).build(), 3);

        assertThat(linea.asignacionFamiliar()).isEqualTo(Dinero.de("113.00"));
    }

    @Test
    void sinHijosElegiblesNoHayAsignacionFamiliar() {
        LineaPlanilla linea = calcular(datos().sinHijos().build(), 3);

        assertThat(linea.asignacionFamiliar()).isEqualTo(Dinero.CERO);
    }

    @Test
    void lasTardanzasYLasFaltasSeDescuentanConElValorMinutoYElValorDia() {
        LineaPlanilla linea = calcular(datos().tardanza(60).faltas(1).build(), 3);

        assertThat(linea.descuentoAsistencia()).isEqualTo(Dinero.de("112.50"));
    }

    @Test
    void elDescuentoDeAsistenciaNoSuperaElBasicoDelMes() {
        LineaPlanilla linea = calcular(datos().dias(2).faltas(30).build(), 3);

        assertThat(linea.descuentoAsistencia()).isEqualTo(linea.basico()).isEqualTo(Dinero.de("193.55"));
    }

    @Test
    void laJornadaDelContratoDefineElValorDeLaHoraExtra() {
        LineaPlanilla linea = calcular(datos().jornada(24).horasExtra("2").build(), 3);

        assertThat(linea.horasExtra()).isEqualTo(Dinero.de("50.00"));
    }

    @Test
    void conAfpUsaLaEstrategiaDelTrabajador() {
        RegimenPensionario afp = new pe.nexosoftware.planilla360.planilla.domain.regimen.Afp("Prima",
                new BigDecimal("1.60"));

        LineaPlanilla linea = calcular(datos().regimen(afp).build(), 3);

        assertThat(linea.regimenPensionario()).isEqualTo(afp);
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.de("403.76"));
    }

    @Test
    void laRetencionDeQuintaProyectaLasDosGratificacionesAntesDeJulio() {
        LineaPlanilla linea = calcular(datos().sinHijos().basico("10000").build(), 1);

        assertThat(linea.retencion5ta()).isEqualTo(Dinero.de("1083.67"));
    }

    @Test
    void despuesDeJulioSoloQuedaLaGratificacionDeDiciembre() {
        LineaPlanilla agosto = calcular(datos().build(), 8);

        assertThat(agosto.retencion5ta()).isEqualTo(Dinero.CERO);
    }

    @Test
    void loYaPercibidoEnElAnioAumentaLaRetencion() {
        LineaPlanilla sinPercibido = calcular(datos().sinHijos().basico("10000").build(), 7);
        LineaPlanilla conPercibido = calcular(datos().sinHijos().basico("10000").percibido("60000").build(), 7);

        assertThat(conPercibido.retencion5ta().compareTo(sinPercibido.retencion5ta())).isPositive();
    }

    @Test
    void laPequenaEmpresaProyectaMediaGratificacion() {
        LineaPlanilla general = calcular(datos().sinHijos().basico("10000").build(), 1);
        LineaPlanilla pequena = calcular(
                datos().sinHijos().basico("10000").laboral(RegimenLaboral.PEQUENA_EMPRESA).build(), 1);

        assertThat(pequena.retencion5ta().compareTo(general.retencion5ta())).isNegative();
    }

    @ParameterizedTest(name = "{0} días laborados en marzo")
    @CsvSource({"0", "-1", "32"})
    void losDiasLaboradosDebenEstarEntreUnoYLosDiasDelMes(int dias) {
        assertThatThrownBy(() -> calcular(datos().dias(dias).build(), 3))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("días laborados");
    }

    @Test
    void lasFaltasYTardanzasNoPuedenSerNegativas() {
        assertThatThrownBy(() -> calcular(datos().faltas(-1).build(), 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calcular(datos().tardanza(-1).build(), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Test data builder de los datos mensuales. */
    private static final class DatosBuilder {

        private Dinero basico = Dinero.de("3000");
        private int jornada = 48;
        private RegimenPensionario regimen = new Onp();
        private RegimenLaboral laboral = RegimenLaboral.GENERAL;
        private List<Hijo> hijos = List.of(HIJO_MENOR);
        private int dias = 31;
        private int minutosTardanza;
        private int faltas;
        private List<BigDecimal> horasExtra = List.of();
        private Dinero percibido = Dinero.CERO;

        DatosBuilder basico(String valor) {
            this.basico = Dinero.de(valor);
            return this;
        }

        DatosBuilder jornada(int horas) {
            this.jornada = horas;
            return this;
        }

        DatosBuilder regimen(RegimenPensionario valor) {
            this.regimen = valor;
            return this;
        }

        DatosBuilder laboral(RegimenLaboral valor) {
            this.laboral = valor;
            return this;
        }

        DatosBuilder sinHijos() {
            this.hijos = List.of();
            return this;
        }

        DatosBuilder dias(int valor) {
            this.dias = valor;
            return this;
        }

        DatosBuilder tardanza(int minutos) {
            this.minutosTardanza = minutos;
            return this;
        }

        DatosBuilder faltas(int valor) {
            this.faltas = valor;
            return this;
        }

        DatosBuilder horasExtra(String horasDeUnDia) {
            this.horasExtra = List.of(new BigDecimal(horasDeUnDia));
            return this;
        }

        DatosBuilder percibido(String valor) {
            this.percibido = Dinero.de(valor);
            return this;
        }

        DatosMensualesTrabajador build() {
            return new DatosMensualesTrabajador(EMPLEADO, basico, jornada, regimen, laboral, hijos, dias,
                    minutosTardanza, faltas, horasExtra, percibido);
        }
    }
}
