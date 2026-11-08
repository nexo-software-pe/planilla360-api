package pe.nexosoftware.planilla360.planilla.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.planilla.application.model.ResultadoCierre;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.Incidencia;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

@DisplayName("Cierre idempotente de la planilla")
class CierrePlanillaServiceTests {

    private static final UUID EMPRESA = new UUID(9, 9);
    private static final Instant AHORA = Instant.parse("2030-04-02T15:00:00Z");

    private final PlanillaPort planillas = mock(PlanillaPort.class);
    private final ValidadorDePlanilla validador = mock(ValidadorDePlanilla.class);
    private final PublicadorEventosPort eventos = mock(PublicadorEventosPort.class);
    private final CierrePlanillaService servicio = new CierrePlanillaService(
            planillas, validador, eventos, Clock.fixed(AHORA, ZoneOffset.UTC));

    private Planilla calculada;

    @BeforeEach
    void planillaCalculada() {
        calculada = Planilla.reconstruir(UUID.randomUUID(), EMPRESA, Periodo.de(2030, 3), TipoPlanilla.MENSUAL,
                EstadoPlanilla.CALCULADA, List.of(linea(1), linea(2)), null, null);
        when(planillas.buscarParaActualizar(calculada.id())).thenReturn(Optional.of(calculada));
        when(planillas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validador.validar(any())).thenReturn(new ResultadoValidacion(List.of()));
    }

    @Test
    void cierraYPublicaElEventoUnaSolaVez() {
        ResultadoCierre resultado = servicio.cerrar(calculada.id(), "  clave-1  ");

        assertThat(resultado.repetido()).isFalse();
        assertThat(resultado.planilla().estado()).isEqualTo(EstadoPlanilla.CERRADA);
        assertThat(resultado.planilla().claveIdempotencia()).contains("clave-1");
        assertThat(resultado.planilla().cerradaEn()).contains(AHORA);
        verify(planillas).guardar(calculada);
        verify(eventos, times(1)).publicar(any());
    }

    @Test
    void repetirConLaMismaClaveNoGuardaNiPublicaNiVuelveAValidar() {
        servicio.cerrar(calculada.id(), "clave-1");
        when(planillas.idConClave("clave-1")).thenReturn(Optional.of(calculada.id()));

        ResultadoCierre repetido = servicio.cerrar(calculada.id(), "clave-1");

        assertThat(repetido.repetido()).isTrue();
        assertThat(repetido.planilla().estado()).isEqualTo(EstadoPlanilla.CERRADA);
        verify(planillas, times(1)).guardar(any());
        verify(eventos, times(1)).publicar(any());
        verify(validador, times(1)).validar(any());
    }

    @Test
    void otraClaveParaUnaPlanillaYaCerradaEsUnConflictoDeDominio() {
        servicio.cerrar(calculada.id(), "clave-1");

        assertThatThrownBy(() -> servicio.cerrar(calculada.id(), "clave-2"))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("otra clave");
    }

    @Test
    void unaClaveYaUsadaEnOtraPlanillaEsUn409() {
        when(planillas.idConClave("clave-1")).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> servicio.cerrar(calculada.id(), "clave-1"))
                .isInstanceOf(ConflictoException.class);
        verify(planillas, never()).guardar(any());
    }

    @Test
    void conIncidenciasNoCierraNiPublica() {
        when(validador.validar(any())).thenReturn(
                new ResultadoValidacion(List.of(new Incidencia("ASISTENCIA_INCOMPLETA", "Falta asistencia"))));

        assertThatThrownBy(() -> servicio.cerrar(calculada.id(), "clave-1"))
                .isInstanceOf(ReglaDePlanillaException.class).hasMessageContaining("Falta asistencia");
        assertThat(calculada.estado()).isEqualTo(EstadoPlanilla.CALCULADA);
        verify(planillas, never()).guardar(any());
        verify(eventos, never()).publicar(any());
    }

    @Test
    void laClaveEsObligatoria() {
        assertThatThrownBy(() -> servicio.cerrar(calculada.id(), null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> servicio.cerrar(calculada.id(), "   ")).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void laClaveTieneUnLargoMaximoDeCien() {
        assertThatThrownBy(() -> servicio.cerrar(calculada.id(), "x".repeat(101)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("100");
        assertThat(servicio.cerrar(calculada.id(), "x".repeat(100)).repetido()).isFalse();
    }

    @Test
    void cerrarUnaPlanillaInexistenteEsUn404() {
        assertThatThrownBy(() -> servicio.cerrar(UUID.randomUUID(), "clave-1"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
