package pe.nexosoftware.planilla360.planilla.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.Incidencia;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

@DisplayName("PlanillaService")
class PlanillaServiceTests {

    private static final UUID EMPRESA = new UUID(9, 9);
    private static final Periodo MARZO = Periodo.de(2030, 3);

    private final PlanillaPort planillas = mock(PlanillaPort.class);
    private final ArmadorDeLineas armador = mock(ArmadorDeLineas.class);
    private final ValidadorDePlanilla validador = mock(ValidadorDePlanilla.class);
    private final PublicadorEventosPort eventos = mock(PublicadorEventosPort.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2030-04-02T15:00:00Z"), ZoneOffset.UTC);
    private final PlanillaService servicio = new PlanillaService(planillas, armador, validador, eventos, reloj);

    @BeforeEach
    void empresaEnContexto() {
        TenantContext.establecer(EMPRESA);
        when(planillas.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        TenantContext.limpiar();
    }

    private Planilla borrador() {
        return Planilla.crearBorrador(UUID.randomUUID(), EMPRESA, MARZO, TipoPlanilla.MENSUAL);
    }

    @Test
    void creaUnBorradorDeLaEmpresaDelContextoYLoAudita() {
        Planilla creada = servicio.crear(MARZO, TipoPlanilla.MENSUAL);

        assertThat(creada.estado()).isEqualTo(EstadoPlanilla.BORRADOR);
        assertThat(creada.empresaId()).isEqualTo(EMPRESA);
        verify(eventos).registrarCambio(any(), org.mockito.ArgumentMatchers.eq(Accion.CREAR), any(), any());
    }

    @Test
    void noCreaUnaSegundaPlanillaVigenteDelMismoPeriodoYTipo() {
        when(planillas.existeVigente(MARZO, TipoPlanilla.MENSUAL)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(MARZO, TipoPlanilla.MENSUAL))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("Anúlela");
        verify(planillas, never()).guardar(any());
    }

    @Test
    void rechazaLosPeriodosFuturos() {
        assertThatThrownBy(() -> servicio.crear(Periodo.de(2030, 5), TipoPlanilla.MENSUAL))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("futuro");
    }

    @Test
    void elMesActualSiEstaPermitido() {
        assertThat(servicio.crear(Periodo.de(2030, 4), TipoPlanilla.MENSUAL).periodo()).isEqualTo(Periodo.de(2030, 4));
    }

    @Test
    void laGratificacionSoloSeCreaEnJulioODiciembre() {
        assertThatThrownBy(() -> servicio.crear(MARZO, TipoPlanilla.GRATIFICACION))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("julio o diciembre");
    }

    @Test
    void laCtsSoloSeCreaEnMayoONoviembre() {
        assertThatThrownBy(() -> servicio.crear(MARZO, TipoPlanilla.CTS))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("mayo o noviembre");
    }

    @Test
    void calcularGuardaLasLineasYPasaLaPlanillaACalculada() {
        Planilla planilla = borrador();
        when(planillas.buscarParaActualizar(planilla.id())).thenReturn(Optional.of(planilla));
        when(armador.armar(MARZO, TipoPlanilla.MENSUAL)).thenReturn(List.of(linea(1), linea(2)));

        Planilla calculada = servicio.calcular(planilla.id());

        assertThat(calculada.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
        assertThat(calculada.lineas()).hasSize(2);
        verify(eventos).registrarCambio(any(), org.mockito.ArgumentMatchers.eq(Accion.ACTUALIZAR), any(), any());
    }

    @Test
    void noCalculaUnaPlanillaCerradaNiSiquieraConsultaOtrosModulos() {
        Planilla cerrada = Planilla.reconstruir(UUID.randomUUID(), EMPRESA, MARZO, TipoPlanilla.MENSUAL,
                EstadoPlanilla.CERRADA, List.of(linea(1)), "clave", Instant.parse("2030-04-01T10:00:00Z"));
        when(planillas.buscarParaActualizar(cerrada.id())).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> servicio.calcular(cerrada.id()))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("CERRADA");
        org.mockito.Mockito.verifyNoInteractions(armador);
    }

    @Test
    void calcularUnaPlanillaInexistenteEsUn404() {
        assertThatThrownBy(() -> servicio.calcular(UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void simularNoGuardaNada() {
        when(armador.armar(MARZO, TipoPlanilla.MENSUAL)).thenReturn(List.of(linea(1)));

        Planilla simulada = servicio.simular(MARZO, TipoPlanilla.MENSUAL);

        assertThat(simulada.lineas()).hasSize(1);
        verify(planillas, never()).guardar(any());
        org.mockito.Mockito.verifyNoInteractions(eventos);
    }

    @Test
    void generarCreaYCalculaEnUnSoloPaso() {
        when(planillas.buscarParaActualizar(any())).thenAnswer(inv -> Optional.of(
                Planilla.crearBorrador(inv.getArgument(0), EMPRESA, MARZO, TipoPlanilla.MENSUAL)));
        when(armador.armar(MARZO, TipoPlanilla.MENSUAL)).thenReturn(List.of(linea(1)));

        Planilla generada = servicio.generar(MARZO, TipoPlanilla.MENSUAL);

        assertThat(generada.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
    }

    @Test
    void obtenerYValidarUnaPlanillaInexistenteEsUn404() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.obtener(id)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.validar(id)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void validarDelegaEnElValidadorConLaPlanillaEncontrada() {
        Planilla planilla = borrador();
        ResultadoValidacion resultado = new ResultadoValidacion(List.of(new Incidencia("X", "mensaje")));
        when(planillas.buscar(planilla.id())).thenReturn(Optional.of(planilla));
        when(validador.validar(planilla)).thenReturn(resultado);

        assertThat(servicio.validar(planilla.id())).isSameAs(resultado);
    }
}
