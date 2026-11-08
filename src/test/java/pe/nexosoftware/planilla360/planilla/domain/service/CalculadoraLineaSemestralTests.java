package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosBeneficioSemestral;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TiempoComputable;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Afp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

@DisplayName("Líneas de gratificación y CTS")
class CalculadoraLineaSemestralTests {

    private static final UUID EMPLEADO = new UUID(0, 11);
    private static final Hijo HIJO_MENOR = new Hijo(LocalDate.of(2015, 6, 1), false);

    private final CalculadoraLineaSemestral calculadora = new CalculadoraLineaSemestral();

    private static DatosBeneficioSemestral datos(TiempoComputable tiempo, RegimenLaboral laboral,
            List<Dinero> horasExtra, Dinero ultimaGratificacion) {
        return new DatosBeneficioSemestral(EMPLEADO, Dinero.de("3000"), new Onp(), laboral, List.of(HIJO_MENOR),
                horasExtra, tiempo, ultimaGratificacion);
    }

    private static DatosBeneficioSemestral datos(TiempoComputable tiempo) {
        return datos(tiempo, RegimenLaboral.GENERAL, List.of(), Dinero.CERO);
    }

    @Test
    void laGratificacionDelSemestreCompletoEsUnaRemuneracionMasLaBonificacion() {
        LineaPlanilla linea = calculadora.gratificacion(datos(new TiempoComputable(6, 0)), Periodo.de(2026, 7),
                referencia());

        assertThat(linea.empleadoId()).isEqualTo(EMPLEADO);
        assertThat(linea.basico()).isEqualTo(Dinero.CERO);
        assertThat(linea.gratificacion()).isEqualTo(Dinero.de("3113.00"));
        assertThat(linea.bonificacionExtraordinaria()).isEqualTo(Dinero.de("280.17"));
        assertThat(linea.descuentoPension().total()).isEqualTo(Dinero.CERO);
        assertThat(linea.retencion5ta()).isEqualTo(Dinero.CERO);
        assertThat(linea.neto()).isEqualTo(Dinero.de("3393.17"));
    }

    @Test
    void laGratificacionEsProporcionalALosMesesCompletos() {
        LineaPlanilla linea = calculadora.gratificacion(datos(new TiempoComputable(3, 20)), Periodo.de(2026, 7),
                referencia());

        assertThat(linea.gratificacion()).isEqualTo(Dinero.de("1556.50"));
        assertThat(linea.bonificacionExtraordinaria()).isEqualTo(Dinero.de("140.09"));
    }

    @Test
    void laPequenaEmpresaPagaLaMitadDeLaGratificacion() {
        LineaPlanilla linea = calculadora.gratificacion(
                datos(new TiempoComputable(6, 0), RegimenLaboral.PEQUENA_EMPRESA, List.of(), Dinero.CERO),
                Periodo.de(2026, 12), referencia());

        assertThat(linea.gratificacion()).isEqualTo(Dinero.de("1556.50"));
    }

    @Test
    void lasHorasExtraDelSemestreEntranAlPromedioSiHuboEnTresMesesOMas() {
        List<Dinero> horasExtra = List.of(Dinero.de("100"), Dinero.de("100"), Dinero.de("100"), Dinero.CERO);

        LineaPlanilla linea = calculadora.gratificacion(
                datos(new TiempoComputable(6, 0), RegimenLaboral.GENERAL, horasExtra, Dinero.CERO),
                Periodo.de(2026, 7), referencia());

        assertThat(linea.gratificacion()).isEqualTo(Dinero.de("3163.00"));
    }

    @Test
    void sinHijosLaAsignacionFamiliarNoEntraEnLaGratificacion() {
        DatosBeneficioSemestral sinHijos = new DatosBeneficioSemestral(EMPLEADO, Dinero.de("3000"), new Onp(),
                RegimenLaboral.GENERAL, List.of(), List.of(), new TiempoComputable(6, 0), Dinero.CERO);

        LineaPlanilla linea = calculadora.gratificacion(sinHijos, Periodo.de(2026, 7), referencia());

        assertThat(linea.gratificacion()).isEqualTo(Dinero.de("3000.00"));
    }

    @Test
    void laRegimenPensionarioDelTrabajadorQuedaEnLaLinea() {
        Afp afp = new Afp("Prima", new java.math.BigDecimal("1.60"));
        DatosBeneficioSemestral conAfp = new DatosBeneficioSemestral(EMPLEADO, Dinero.de("3000"), afp,
                RegimenLaboral.GENERAL, List.of(), List.of(), new TiempoComputable(6, 0), Dinero.CERO);

        LineaPlanilla linea = calculadora.gratificacion(conAfp, Periodo.de(2026, 7), referencia());

        assertThat(linea.regimenPensionario()).isEqualTo(afp);
    }

    @Test
    void laCtsDelSemestreCompletoEsMediaRemuneracionComputable() {
        DatosBeneficioSemestral datos = datos(new TiempoComputable(6, 0), RegimenLaboral.GENERAL, List.of(),
                Dinero.de("3113"));

        LineaPlanilla linea = calculadora.cts(datos, Periodo.de(2026, 5), referencia());

        assertThat(linea.empleadoId()).isEqualTo(EMPLEADO);
        assertThat(linea.cts()).isEqualTo(Dinero.de("1815.92"));
        assertThat(linea.gratificacion()).isEqualTo(Dinero.CERO);
        assertThat(linea.basico()).isEqualTo(Dinero.CERO);
        assertThat(linea.neto()).isEqualTo(Dinero.de("1815.92"));
    }

    @Test
    void laCtsDeLaPequenaEmpresaEsLaMitad() {
        DatosBeneficioSemestral datos = datos(new TiempoComputable(6, 0), RegimenLaboral.PEQUENA_EMPRESA,
                List.of(), Dinero.de("3113"));

        LineaPlanilla linea = calculadora.cts(datos, Periodo.de(2026, 11), referencia());

        assertThat(linea.cts()).isEqualTo(Dinero.de("907.96"));
    }

    @Test
    void laCtsIncluyeLosDiasCompletos() {
        DatosBeneficioSemestral datos = datos(new TiempoComputable(5, 15), RegimenLaboral.GENERAL, List.of(),
                Dinero.de("3113"));

        LineaPlanilla linea = calculadora.cts(datos, Periodo.de(2026, 5), referencia());

        assertThat(linea.cts()).isEqualTo(Dinero.de("1664.59"));
    }

    @Test
    void sinGratificacionPreviaLaCtsSoloUsaLaRemuneracionBase() {
        LineaPlanilla linea = calculadora.cts(datos(new TiempoComputable(6, 0)), Periodo.de(2026, 5), referencia());

        assertThat(linea.cts()).isEqualTo(Dinero.de("1556.50"));
    }
}
