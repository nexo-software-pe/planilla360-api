package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

@DisplayName("Planilla reconstruida desde el almacenamiento")
class PlanillaReconstruccionTests {

    private static final UUID ID = new UUID(1, 1);
    private static final UUID EMPRESA = new UUID(2, 2);
    private static final Periodo PERIODO = Periodo.de(2026, 3);
    private static final Instant CIERRE = Instant.parse("2026-04-01T14:00:00Z");

    private static Planilla reconstruir(EstadoPlanilla estado, List<LineaPlanilla> lineas, String clave,
            Instant cerradaEn) {
        return Planilla.reconstruir(ID, EMPRESA, PERIODO, TipoPlanilla.MENSUAL, estado, lineas, clave, cerradaEn);
    }

    @Test
    void conservaTodosSusDatos() {
        Planilla planilla = reconstruir(EstadoPlanilla.CERRADA, List.of(linea(1), linea(2)), "clave-1", CIERRE);

        assertThat(planilla.id()).isEqualTo(ID);
        assertThat(planilla.empresaId()).isEqualTo(EMPRESA);
        assertThat(planilla.periodo()).isEqualTo(PERIODO);
        assertThat(planilla.tipo()).isEqualTo(TipoPlanilla.MENSUAL);
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CERRADA);
        assertThat(planilla.lineas()).hasSize(2);
        assertThat(planilla.claveIdempotencia()).contains("clave-1");
        assertThat(planilla.cerradaEn()).contains(CIERRE);
        assertThat(planilla.totalNeto()).isEqualTo(Dinero.de("5220.00"));
    }

    @Test
    void unBorradorPuedeNoTenerLineas() {
        Planilla planilla = reconstruir(EstadoPlanilla.BORRADOR, List.of(), null, null);

        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.BORRADOR);
        assertThat(planilla.lineas()).isEmpty();
        assertThat(planilla.claveIdempotencia()).isEmpty();
        assertThat(planilla.cerradaEn()).isEmpty();
    }

    @Test
    void repetirElCierreConLaMismaClaveNoHaceNada() {
        Planilla planilla = reconstruir(EstadoPlanilla.CERRADA, List.of(linea(1)), "clave-1", CIERRE);

        assertThat(planilla.cerrar("clave-1", new ResultadoValidacion(List.of()), Instant.now())).isEmpty();
        assertThat(planilla.cerradaEn()).contains(CIERRE);
    }

    @Test
    void cerrarConOtraClaveEsUnConflicto() {
        Planilla planilla = reconstruir(EstadoPlanilla.CERRADA, List.of(linea(1)), "clave-1", CIERRE);

        assertThatThrownBy(() -> planilla.cerrar("otra", new ResultadoValidacion(List.of()), Instant.now()))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("otra clave");
    }

    @Test
    void unaPlanillaCalculadaSeCierraConNormalidad() {
        Planilla planilla = reconstruir(EstadoPlanilla.CALCULADA, List.of(linea(1)), null, null);

        assertThat(planilla.cerrar("clave-2", new ResultadoValidacion(List.of()), CIERRE)).isPresent();
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CERRADA);
    }

    @Test
    void unaPlanillaCerradaSePuedeAnular() {
        Planilla planilla = reconstruir(EstadoPlanilla.CERRADA, List.of(linea(1)), "clave-1", CIERRE);

        planilla.anular();

        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.ANULADA);
    }

    @Test
    void unaPlanillaAnuladaConservaLosDatosDeSuCierre() {
        Planilla planilla = reconstruir(EstadoPlanilla.ANULADA, List.of(linea(1)), "clave-1", CIERRE);

        assertThat(planilla.claveIdempotencia()).contains("clave-1");
        assertThat(planilla.cerradaEn()).contains(CIERRE);
    }

    @Test
    void unaPlanillaAnuladaDesdeBorradorNoTieneCierre() {
        Planilla planilla = reconstruir(EstadoPlanilla.ANULADA, List.of(), null, null);

        assertThat(planilla.cerradaEn()).isEmpty();
    }

    @Test
    void unaPlanillaCalculadaDebeTenerLineas() {
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.CALCULADA, List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("líneas");
    }

    @Test
    void unaPlanillaCerradaDebeTenerLineas() {
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.CERRADA, List.of(), "clave-1", CIERRE))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("líneas");
    }

    @Test
    void unaPlanillaCerradaDebeTenerClaveYFechaDeCierre() {
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.CERRADA, List.of(linea(1)), null, CIERRE))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("clave");
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.CERRADA, List.of(linea(1)), "clave-1", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fecha de cierre");
    }

    @Test
    void unBorradorOCalculadaNoTienenDatosDeCierre() {
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.BORRADOR, List.of(), "clave-1", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no cerrada");
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.CALCULADA, List.of(linea(1)), null, CIERRE))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no cerrada");
    }

    @Test
    void noAdmiteDosLineasDelMismoTrabajador() {
        assertThatThrownBy(() -> reconstruir(EstadoPlanilla.CALCULADA, List.of(linea(1), linea(1)), null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("más de una línea");
    }

    @Test
    void laListaDeLineasNoSePuedeModificarPorFuera() {
        List<LineaPlanilla> origen = new java.util.ArrayList<>(List.of(linea(1)));
        Planilla planilla = reconstruir(EstadoPlanilla.CALCULADA, origen, null, null);

        origen.add(linea(2));

        assertThat(planilla.lineas()).hasSize(1);
    }
}
