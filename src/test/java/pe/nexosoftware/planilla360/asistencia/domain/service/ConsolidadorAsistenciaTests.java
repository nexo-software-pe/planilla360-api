package pe.nexosoftware.planilla360.asistencia.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.DESCANSO;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.FALTA;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.FERIADO;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.INCOMPLETA;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.JUSTIFICADA;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.PRESENTE;
import static pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia.TARDANZA;
import static pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion.ENTRADA;
import static pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion.SALIDA;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.domain.model.ContextoDia;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

class ConsolidadorAsistenciaTests {

    /** Martes. */
    private static final LocalDate MARTES = LocalDate.of(2026, 3, 10);

    private static final LocalDate SABADO = LocalDate.of(2026, 3, 14);
    private static final Horario OFICINA = Horario.lunesAViernes(LocalTime.of(8, 0), LocalTime.of(17, 0), 10);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ConsolidadorAsistencia consolidador = new ConsolidadorAsistencia();

    private static Marcacion marca(LocalDate fecha, String hora, TipoMarcacion tipo) {
        LocalTime t = LocalTime.parse(hora);
        return new Marcacion(LocalDateTime.of(fecha, t).atZone(LIMA).toInstant(), tipo);
    }

    private static ContextoDia dia(LocalDate fecha, boolean feriado, boolean justificado, Marcacion... marcas) {
        return new ContextoDia(fecha, OFICINA, feriado, justificado, List.of(marcas));
    }

    private AsistenciaDiaria consolidar(Marcacion... marcas) {
        return consolidador.consolidar(dia(MARTES, false, false, marcas));
    }

    @Test
    void unDiaNoLaborableEsDescansoAunqueHayaMarcaciones() {
        AsistenciaDiaria resultado = consolidador.consolidar(
                dia(SABADO, false, false, marca(SABADO, "09:00", ENTRADA), marca(SABADO, "13:00", SALIDA)));

        assertThat(resultado).isEqualTo(new AsistenciaDiaria(SABADO, 0, 0, DESCANSO));
    }

    @Test
    void unFeriadoEsFeriadoConOSinMarcaciones() {
        assertThat(consolidador.consolidar(dia(MARTES, true, false)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, FERIADO));
        assertThat(consolidador.consolidar(
                        dia(MARTES, true, false, marca(MARTES, "08:30", ENTRADA), marca(MARTES, "18:00", SALIDA))))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, FERIADO));
    }

    @Test
    void elFeriadoPrimaSobreLaJustificacion() {
        assertThat(consolidador.consolidar(dia(MARTES, true, true)).estado()).isEqualTo(FERIADO);
    }

    @Test
    void sinMarcacionesNiJustificacionEsFalta() {
        assertThat(consolidador.consolidar(dia(MARTES, false, false)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, FALTA));
    }

    @Test
    void sinMarcacionesPeroJustificadoEsJustificada() {
        assertThat(consolidador.consolidar(dia(MARTES, false, true)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, JUSTIFICADA));
    }

    @Test
    void llegarAHoraYSalirAHoraEsPresenteSinMinutosExtra() {
        assertThat(consolidar(marca(MARTES, "07:55", ENTRADA), marca(MARTES, "17:00", SALIDA)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, PRESENTE));
    }

    @ParameterizedTest(name = "entrada {0} -> {1} min de tardanza")
    @CsvSource({
        "08:00, 0",
        "08:09, 0",
        "08:10, 0",
        "08:10:59, 0",
        "08:11, 11",
        "08:11:30, 11",
        "08:45, 45",
        "10:00, 120"
    })
    void laTardanzaSeCuentaDesdeLaHoraDeEntradaSoloSiSuperaLaTolerancia(String entrada, int minutos) {
        AsistenciaDiaria resultado = consolidar(marca(MARTES, entrada, ENTRADA), marca(MARTES, "17:00", SALIDA));

        assertThat(resultado.minutosTardanza()).isEqualTo(minutos);
        assertThat(resultado.estado()).isEqualTo(minutos == 0 ? PRESENTE : TARDANZA);
    }

    @ParameterizedTest(name = "salida {0} -> {1} min extra")
    @CsvSource({"16:00, 0", "16:59, 0", "17:00, 0", "17:00:59, 0", "17:01, 1", "17:30, 30", "19:15, 135"})
    void losMinutosExtraSonLosPosterioresALaHoraDeSalida(String salida, int minutos) {
        AsistenciaDiaria resultado = consolidar(marca(MARTES, "08:00", ENTRADA), marca(MARTES, salida, SALIDA));

        assertThat(resultado.minutosExtra()).isEqualTo(minutos);
        assertThat(resultado.estado()).isEqualTo(PRESENTE);
    }

    @Test
    void puedeHaberTardanzaYMinutosExtraEnElMismoDia() {
        assertThat(consolidar(marca(MARTES, "08:30", ENTRADA), marca(MARTES, "18:00", SALIDA)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 30, 60, TARDANZA));
    }

    @Test
    void conVariasMarcacionesSeUsaLaPrimeraEntradaYLaUltimaSalida() {
        AsistenciaDiaria resultado = consolidar(
                marca(MARTES, "14:00", ENTRADA),
                marca(MARTES, "13:00", SALIDA),
                marca(MARTES, "08:00", ENTRADA),
                marca(MARTES, "18:00", SALIDA));

        assertThat(resultado).isEqualTo(new AsistenciaDiaria(MARTES, 0, 60, PRESENTE));
    }

    @Test
    void sinSalidaLaAsistenciaQuedaIncompletaConsuTardanzaYSinExtra() {
        assertThat(consolidar(marca(MARTES, "08:20", ENTRADA)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 20, 0, INCOMPLETA));
        assertThat(consolidar(marca(MARTES, "08:00", ENTRADA)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, INCOMPLETA));
    }

    @Test
    void sinEntradaLaAsistenciaQuedaIncompleta() {
        assertThat(consolidar(marca(MARTES, "17:30", SALIDA)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, INCOMPLETA));
    }

    @Test
    void unaSalidaAnteriorALaEntradaNoCuentaComoSalida() {
        assertThat(consolidar(marca(MARTES, "07:00", SALIDA), marca(MARTES, "08:00", ENTRADA)))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, INCOMPLETA));
    }

    @Test
    void soloCuentanLasMarcacionesDeLaFechaEnHoraDeLima() {
        // 23:30 en Lima del día 9 es el día 10 en UTC; 23:30 del día 10 en Lima es el día 11 en UTC.
        Marcacion anterior = marca(MARTES.minusDays(1), "23:30", SALIDA);
        Marcacion posterior = marca(MARTES, "23:30", SALIDA);

        assertThat(consolidar(anterior)).isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, FALTA));
        assertThat(consolidar(marca(MARTES, "08:00", ENTRADA), posterior))
                .isEqualTo(new AsistenciaDiaria(MARTES, 0, 390, PRESENTE));
    }

    @Test
    void laZonaHorariaEsConfigurableYPorDefectoEsLima() {
        Marcacion entradaUtc = new Marcacion(MARTES.atTime(13, 0).toInstant(ZoneOffset.UTC), ENTRADA);
        Marcacion salidaUtc = new Marcacion(MARTES.atTime(22, 0).toInstant(ZoneOffset.UTC), SALIDA);
        ContextoDia contexto = new ContextoDia(MARTES, OFICINA, false, false, List.of(entradaUtc, salidaUtc));

        // 13:00 UTC = 08:00 Lima
        assertThat(consolidador.consolidar(contexto)).isEqualTo(new AsistenciaDiaria(MARTES, 0, 0, PRESENTE));
        // 13:00 UTC en UTC es 13:00: llega 5 horas tarde
        assertThat(new ConsolidadorAsistencia(ZoneOffset.UTC).consolidar(contexto).minutosTardanza()).isEqualTo(300);
        assertThat(ConsolidadorAsistencia.ZONA_LIMA).isEqualTo(LIMA);
    }
}
