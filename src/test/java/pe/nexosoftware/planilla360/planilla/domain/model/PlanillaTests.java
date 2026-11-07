package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.lineaConBasico;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

@DisplayName("Planilla (agregado)")
class PlanillaTests {

    private static final UUID ID = new UUID(1, 1);
    private static final UUID EMPRESA = new UUID(2, 2);
    private static final Periodo PERIODO = Periodo.de(2026, 10);

    private static Planilla borrador() {
        return Planilla.crearBorrador(ID, EMPRESA, PERIODO, TipoPlanilla.MENSUAL);
    }

    @Test
    void naceEnBorradorSinLineas() {
        Planilla planilla = borrador();
        assertThat(planilla.id()).isEqualTo(ID);
        assertThat(planilla.empresaId()).isEqualTo(EMPRESA);
        assertThat(planilla.periodo()).isEqualTo(PERIODO);
        assertThat(planilla.tipo()).isEqualTo(TipoPlanilla.MENSUAL);
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.BORRADOR);
        assertThat(planilla.lineas()).isEmpty();
        assertThat(planilla.totalNeto()).isEqualTo(Dinero.CERO);
    }

    @Test
    void exigeTodosSusDatos() {
        assertThatNullPointerException().isThrownBy(
                () -> Planilla.crearBorrador(null, EMPRESA, PERIODO, TipoPlanilla.MENSUAL));
        assertThatNullPointerException().isThrownBy(
                () -> Planilla.crearBorrador(ID, null, PERIODO, TipoPlanilla.MENSUAL));
        assertThatNullPointerException().isThrownBy(
                () -> Planilla.crearBorrador(ID, EMPRESA, null, TipoPlanilla.MENSUAL));
        assertThatNullPointerException().isThrownBy(() -> Planilla.crearBorrador(ID, EMPRESA, PERIODO, null));
    }

    @Test
    void registrarElCalculoLaPasaACalculada() {
        Planilla planilla = borrador();
        planilla.registrarCalculo(List.of(linea(1), linea(2)));
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
        assertThat(planilla.lineas()).hasSize(2);
    }

    @Test
    void sumaLosTotalesDeSusLineas() {
        Planilla planilla = borrador();
        planilla.registrarCalculo(List.of(linea(1), lineaConBasico(2, "2000")));
        assertThat(planilla.totalIngresos()).isEqualTo(Dinero.de("5000.00"));
        assertThat(planilla.totalDescuentos()).isEqualTo(Dinero.de("650.00"));
        assertThat(planilla.totalNeto()).isEqualTo(Dinero.de("4350.00"));
        assertThat(planilla.totalEssalud()).isEqualTo(Dinero.de("450.00"));
    }

    @Test
    void recalcularReemplazaLasLineasAnteriores() {
        Planilla planilla = borrador();
        planilla.registrarCalculo(List.of(linea(1), linea(2)));
        planilla.registrarCalculo(List.of(linea(3)));
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
        assertThat(planilla.lineas()).containsExactly(linea(3));
    }

    @Test
    void noAdmiteDosLineasDelMismoTrabajador() {
        Planilla planilla = borrador();
        List<LineaPlanilla> repetidas = List.of(linea(1), lineaConBasico(1, "2500"));
        assertThatThrownBy(() -> planilla.registrarCalculo(repetidas)).isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("más de una línea");
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.BORRADOR);
    }

    @Test
    void noSePuedeCalcularSinTrabajadores() {
        Planilla planilla = borrador();
        List<LineaPlanilla> vacia = List.of();
        assertThatThrownBy(() -> planilla.registrarCalculo(vacia)).isInstanceOf(ReglaDePlanillaException.class);
    }

    @Test
    void lasLineasNoSePuedenModificarPorFuera() {
        Planilla planilla = borrador();
        planilla.registrarCalculo(List.of(linea(1)));
        assertThatThrownBy(() -> planilla.lineas().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void elCalculoNoGuardaLaListaDelLlamador() {
        Planilla planilla = borrador();
        List<LineaPlanilla> origen = new java.util.ArrayList<>(List.of(linea(1)));
        planilla.registrarCalculo(origen);
        origen.add(linea(2));
        assertThat(planilla.lineas()).hasSize(1);
    }

    @Test
    void sePuedeAnularUnBorrador() {
        Planilla planilla = borrador();
        planilla.anular();
        assertThat(planilla.estado()).isEqualTo(EstadoPlanilla.ANULADA);
    }

    @Test
    void unaPlanillaAnuladaNoSeRecalcula() {
        Planilla planilla = borrador();
        planilla.anular();
        List<LineaPlanilla> lineas = List.of(linea(1));
        assertThatThrownBy(() -> planilla.registrarCalculo(lineas)).isInstanceOf(ReglaDePlanillaException.class);
        assertThatThrownBy(planilla::anular).isInstanceOf(ReglaDePlanillaException.class);
    }
}
