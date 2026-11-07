package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.event.PlanillaCerrada;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;
import pe.nexosoftware.planilla360.planilla.domain.validation.Incidencia;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

@DisplayName("Cierre de planilla (idempotente)")
class PlanillaCierreTests {

    private static final UUID ID = new UUID(1, 1);
    private static final UUID EMPRESA = new UUID(2, 2);
    private static final Instant AHORA = Instant.parse("2026-10-31T23:00:00Z");
    private static final ResultadoValidacion VALIDA = new ResultadoValidacion(List.of());
    private static final String CLAVE = "empresa-2:2026-10:MENSUAL";

    private static Planilla calculada() {
        Planilla planilla = Planilla.crearBorrador(ID, EMPRESA, Periodo.de(2026, 10), TipoPlanilla.MENSUAL);
        planilla.registrarCalculo(List.of(linea(1), linea(2)));
        return planilla;
    }

    @Test
    void cierraYPublicaElEventoConLosDatosDeLaPlanilla() {
        Planilla planilla = calculada();

        PlanillaCerrada evento = planilla.cerrar(CLAVE, VALIDA, AHORA).orElseThrow();

        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CERRADA);
        assertThat(planilla.claveIdempotencia()).contains(CLAVE);
        assertThat(planilla.cerradaEn()).contains(AHORA);
        assertThat(evento.planillaId()).isEqualTo(ID);
        assertThat(evento.empresaId()).isEqualTo(EMPRESA);
        assertThat(evento.periodo()).isEqualTo(Periodo.de(2026, 10));
        assertThat(evento.tipo()).isEqualTo(TipoPlanilla.MENSUAL);
        assertThat(evento.cerradaEn()).isEqualTo(AHORA);
        assertThat(evento.trabajadores()).isEqualTo(2);
        assertThat(evento.totalNeto()).isEqualTo(Dinero.de("5220.00"));
    }

    @Test
    void antesDeCerrarNoTieneClaveNiFechaDeCierre() {
        Planilla planilla = calculada();
        assertThat(planilla.claveIdempotencia()).isEmpty();
        assertThat(planilla.cerradaEn()).isEmpty();
    }

    @Test
    void repetirElCierreConLaMismaClaveNoHaceNadaNiRepiteElEvento() {
        Planilla planilla = calculada();
        planilla.cerrar(CLAVE, VALIDA, AHORA);

        ResultadoValidacion conOtraPlanillaCerrada = new ResultadoValidacion(
                List.of(new Incidencia("PLANILLA_DUPLICADA", "ya existe")));
        var repetido = planilla.cerrar(CLAVE, conOtraPlanillaCerrada, AHORA.plusSeconds(60));

        assertThat(repetido).isEmpty();
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CERRADA);
        assertThat(planilla.cerradaEn()).contains(AHORA);
    }

    @Test
    void cerrarConOtraClaveUnaPlanillaYaCerradaEsUnConflicto() {
        Planilla planilla = calculada();
        planilla.cerrar(CLAVE, VALIDA, AHORA);
        assertThatThrownBy(() -> planilla.cerrar("otra-clave", VALIDA, AHORA))
                .isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("ya está cerrada");
        assertThat(planilla.claveIdempotencia()).contains(CLAVE);
    }

    @Test
    void conIncidenciasNoCierraYListaTodasEnElMensaje() {
        Planilla planilla = calculada();
        ResultadoValidacion conProblemas = new ResultadoValidacion(List.of(
                new Incidencia("ASISTENCIA_INCOMPLETA", "asistencia incompleta"),
                new Incidencia("NETO_NEGATIVO", "neto negativo")));

        assertThatThrownBy(() -> planilla.cerrar(CLAVE, conProblemas, AHORA))
                .isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("asistencia incompleta")
                .hasMessageContaining("neto negativo");
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
        assertThat(planilla.claveIdempotencia()).isEmpty();
        assertThat(planilla.cerradaEn()).isEmpty();
    }

    @Test
    void unBorradorSinCalcularNoSePuedeCerrar() {
        Planilla borrador = Planilla.crearBorrador(ID, EMPRESA, Periodo.de(2026, 10), TipoPlanilla.MENSUAL);
        assertThatThrownBy(() -> borrador.cerrar(CLAVE, VALIDA, AHORA)).isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("BORRADOR");
        assertThat(borrador.claveIdempotencia()).isEmpty();
    }

    @Test
    void unaPlanillaAnuladaNoSePuedeCerrar() {
        Planilla planilla = calculada();
        planilla.anular();
        assertThatThrownBy(() -> planilla.cerrar(CLAVE, VALIDA, AHORA)).isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("ANULADA");
    }

    @Test
    void despuesDeCerrarLasLineasSonInmutables() {
        Planilla planilla = calculada();
        planilla.cerrar(CLAVE, VALIDA, AHORA);
        List<LineaPlanilla> otras = List.of(linea(9));
        assertThatThrownBy(() -> planilla.registrarCalculo(otras)).isInstanceOf(ReglaDePlanillaException.class);
        assertThat(planilla.lineas()).hasSize(2);
    }

    @Test
    void unaPlanillaCerradaTodaviaSePuedeAnularAntesDelPago() {
        Planilla planilla = calculada();
        planilla.cerrar(CLAVE, VALIDA, AHORA);
        planilla.anular();
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.ANULADA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void exigeUnaClaveDeIdempotencia(String clave) {
        Planilla planilla = calculada();
        assertThatThrownBy(() -> planilla.cerrar(clave, VALIDA, AHORA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeClaveValidacionYFecha() {
        Planilla planilla = calculada();
        assertThatNullPointerException().isThrownBy(() -> planilla.cerrar(null, VALIDA, AHORA));
        assertThatNullPointerException().isThrownBy(() -> planilla.cerrar(CLAVE, null, AHORA));
        assertThatNullPointerException().isThrownBy(() -> planilla.cerrar(CLAVE, VALIDA, null));
    }
}
