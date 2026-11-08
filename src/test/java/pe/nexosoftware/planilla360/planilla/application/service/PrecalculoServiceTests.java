package pe.nexosoftware.planilla360.planilla.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.port.in.GestionarPlanillasUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaCatalogoPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

@DisplayName("Pre-cálculo y recordatorios")
class PrecalculoServiceTests {

    private static final LocalDate ULTIMO_DIA_HABIL = LocalDate.of(2030, 3, 29);

    private final GestionarPlanillasUseCase planillas = mock(GestionarPlanillasUseCase.class);
    private final PlanillaPort repositorio = mock(PlanillaPort.class);
    private final ConsultaCatalogoPort catalogo = mock(ConsultaCatalogoPort.class);
    private final PrecalculoService servicio = new PrecalculoService(planillas, repositorio, catalogo);

    @Test
    void elUltimoDiaHabilGeneraLaPlanillaMensualEnBorrador() {
        Planilla generada = Planilla.reconstruir(UUID.randomUUID(), new UUID(9, 9), Periodo.de(2030, 3),
                TipoPlanilla.MENSUAL, EstadoPlanilla.CALCULADA, List.of(linea(1)), null, null);
        when(planillas.generar(Periodo.de(2030, 3), TipoPlanilla.MENSUAL)).thenReturn(generada);

        assertThat(servicio.precalcularSiCorresponde(ULTIMO_DIA_HABIL)).contains(generada.id());
    }

    @Test
    void otroDiaDelMesNoHaceNada() {
        assertThat(servicio.precalcularSiCorresponde(LocalDate.of(2030, 3, 28))).isEmpty();
        assertThat(servicio.precalcularSiCorresponde(LocalDate.of(2030, 3, 31))).isEmpty();
        verify(planillas, never()).generar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void siElUltimoDiaHabilCaeAntesPorUnFeriadoSeUsaElDiaAnterior() {
        when(catalogo.esFeriado(ULTIMO_DIA_HABIL)).thenReturn(true);

        assertThat(servicio.precalcularSiCorresponde(ULTIMO_DIA_HABIL)).isEmpty();
        when(planillas.generar(Periodo.de(2030, 3), TipoPlanilla.MENSUAL)).thenReturn(
                Planilla.crearBorrador(UUID.randomUUID(), new UUID(9, 9), Periodo.de(2030, 3), TipoPlanilla.MENSUAL));
        assertThat(servicio.precalcularSiCorresponde(LocalDate.of(2030, 3, 28))).isPresent();
    }

    @Test
    void siYaHayUnaPlanillaVigenteDelMesNoLaToca() {
        when(repositorio.existeVigente(Periodo.de(2030, 3), TipoPlanilla.MENSUAL)).thenReturn(true);

        assertThat(servicio.precalcularSiCorresponde(ULTIMO_DIA_HABIL)).isEmpty();
        verify(planillas, never()).generar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest(name = "mes {0}: {1}")
    @CsvSource({
        "7, gratificación", "12, gratificación", "5, CTS", "11, CTS"})
    void recuerdaLaGratificacionYLaCts(int mes, String beneficio) {
        assertThat(servicio.recordatorioDeBeneficio(LocalDate.of(2030, mes, 1)))
                .hasValueSatisfying(aviso -> assertThat(aviso).contains(beneficio).contains("2030-" + "%02d".formatted(mes)));
    }

    @ParameterizedTest
    @CsvSource({"1", "2", "3", "4", "6", "8", "9", "10"})
    void losDemasMesesNoTienenRecordatorio(int mes) {
        assertThat(servicio.recordatorioDeBeneficio(LocalDate.of(2030, mes, 1))).isEmpty();
    }
}
