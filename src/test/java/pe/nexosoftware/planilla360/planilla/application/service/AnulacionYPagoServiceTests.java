package pe.nexosoftware.planilla360.planilla.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.planilla.application.model.Actor;
import pe.nexosoftware.planilla360.planilla.application.model.PagoPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.out.PagoPlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

@DisplayName("Anulación y pago de la planilla")
class AnulacionYPagoServiceTests {

    private static final UUID EMPRESA = new UUID(9, 9);
    private static final Actor ADMIN = new Actor(new UUID(1, 1), Rol.ADMIN_EMPRESA);

    private final PlanillaPort planillas = mock(PlanillaPort.class);
    private final PagoPlanillaPort pagos = mock(PagoPlanillaPort.class);
    private final PublicadorEventosPort eventos = mock(PublicadorEventosPort.class);
    private final AnulacionPlanillaService anulacion = new AnulacionPlanillaService(planillas, pagos, eventos);
    private final PagoPlanillaService pago = new PagoPlanillaService(planillas, pagos, eventos);

    private Planilla cerrada;

    @BeforeEach
    void planillaCerrada() {
        cerrada = Planilla.reconstruir(UUID.randomUUID(), EMPRESA, Periodo.de(2030, 3), TipoPlanilla.MENSUAL,
                EstadoPlanilla.CERRADA, List.of(linea(1)), "clave", Instant.parse("2030-04-01T10:00:00Z"));
        when(planillas.buscarParaActualizar(cerrada.id())).thenReturn(Optional.of(cerrada));
        when(planillas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void elAdministradorAnulaUnaPlanillaCerradaSinPago() {
        Planilla anulada = anulacion.anular(ADMIN, cerrada.id(), "  Error de cálculo ");

        assertThat(anulada.estado()).isEqualTo(EstadoPlanilla.ANULADA);
        verify(planillas).guardar(cerrada);
        verify(eventos).registrarCambio(any(), any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "ADMIN_EMPRESA", mode = EnumSource.Mode.EXCLUDE)
    void ningunOtroRolPuedeAnular(Rol rol) {
        assertThatThrownBy(() -> anulacion.anular(new Actor(UUID.randomUUID(), rol), cerrada.id(), "Motivo"))
                .isInstanceOf(AccesoProhibidoException.class);
        assertThat(cerrada.estado()).isEqualTo(EstadoPlanilla.CERRADA);
        verify(planillas, never()).guardar(any());
    }

    @Test
    void conUnPagoRegistradoNoSeAnula() {
        when(pagos.buscar(cerrada.id())).thenReturn(
                Optional.of(new PagoPlanilla(cerrada.id(), LocalDate.of(2030, 4, 2), "OP-1")));

        assertThatThrownBy(() -> anulacion.anular(ADMIN, cerrada.id(), "Motivo"))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("pago");
        assertThat(cerrada.estado()).isEqualTo(EstadoPlanilla.CERRADA);
    }

    @Test
    void elMotivoEsObligatorio() {
        assertThatThrownBy(() -> anulacion.anular(ADMIN, cerrada.id(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> anulacion.anular(ADMIN, cerrada.id(), "  "))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void anularUnaPlanillaInexistenteEsUn404() {
        assertThatThrownBy(() -> anulacion.anular(ADMIN, UUID.randomUUID(), "Motivo"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void registraElPagoDeUnaPlanillaCerrada() {
        PagoPlanilla registrado = pago.registrar(cerrada.id(), LocalDate.of(2030, 4, 2), "  OP-1  ");

        assertThat(registrado.referencia()).isEqualTo("OP-1");
        assertThat(registrado.planillaId()).isEqualTo(cerrada.id());
        verify(eventos).registrarCambio(any(), any(), any(), any());
    }

    @Test
    void elPagoSoloSeRegistraUnaVez() {
        when(pagos.buscar(cerrada.id())).thenReturn(
                Optional.of(new PagoPlanilla(cerrada.id(), LocalDate.of(2030, 4, 2), "OP-1")));

        assertThatThrownBy(() -> pago.registrar(cerrada.id(), LocalDate.of(2030, 4, 3), "OP-2"))
                .isInstanceOf(ConflictoException.class);
        verify(pagos, never()).guardar(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPlanilla.class, names = "CERRADA", mode = EnumSource.Mode.EXCLUDE)
    void soloSePagaUnaPlanillaCerrada(EstadoPlanilla estado) {
        Planilla otra = estado == EstadoPlanilla.BORRADOR
                ? Planilla.crearBorrador(UUID.randomUUID(), EMPRESA, Periodo.de(2030, 3), TipoPlanilla.MENSUAL)
                : Planilla.reconstruir(UUID.randomUUID(), EMPRESA, Periodo.de(2030, 3), TipoPlanilla.MENSUAL, estado,
                        estado == EstadoPlanilla.ANULADA ? List.of() : List.of(linea(1)), null, null);
        when(planillas.buscarParaActualizar(otra.id())).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> pago.registrar(otra.id(), LocalDate.of(2030, 4, 2), "OP-1"))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("cerrada");
    }

    @Test
    void laReferenciaDelPagoEsObligatoria() {
        assertThatThrownBy(() -> pago.registrar(cerrada.id(), LocalDate.of(2030, 4, 2), " "))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void pagarUnaPlanillaInexistenteEsUn404() {
        assertThatThrownBy(() -> pago.registrar(UUID.randomUUID(), LocalDate.of(2030, 4, 2), "OP-1"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
