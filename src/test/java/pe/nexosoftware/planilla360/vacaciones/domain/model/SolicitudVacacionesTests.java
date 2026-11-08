package pe.nexosoftware.planilla360.vacaciones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.vacaciones.domain.excepcion.ReglaDeVacacionesException;

class SolicitudVacacionesTests {

    private static final LocalDate HOY = LocalDate.of(2030, 3, 1);
    private static final UUID EMPLEADO = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();

    private static SolicitudVacaciones solicitud(String desde, String hasta, int saldo) {
        return SolicitudVacaciones.solicitar(
                EMPLEADO, PERIODO, LocalDate.parse(desde), LocalDate.parse(hasta), saldo, HOY);
    }

    /** Siete días: del 10 al 16 de abril. */
    private static SolicitudVacaciones solicitudDeSieteDias() {
        return solicitud("2030-04-10", "2030-04-16", 30);
    }

    private static SolicitudVacaciones en(EstadoSolicitud estado) {
        return SolicitudVacaciones.reconstruir(
                UUID.randomUUID(), EMPLEADO, PERIODO, LocalDate.of(2030, 4, 10), LocalDate.of(2030, 4, 16), estado, null);
    }

    @Test
    void unaSolicitudNuevaQuedaSolicitadaConSusDatos() {
        SolicitudVacaciones s = solicitudDeSieteDias();

        assertThat(s.id()).isNotNull();
        assertThat(s.empleadoId()).isEqualTo(EMPLEADO);
        assertThat(s.periodoId()).isEqualTo(PERIODO);
        assertThat(s.desde()).isEqualTo(LocalDate.of(2030, 4, 10));
        assertThat(s.hasta()).isEqualTo(LocalDate.of(2030, 4, 16));
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.SOLICITADA);
        assertThat(s.motivoRechazo()).isNull();
        assertThat(s.dias()).isEqualTo(7);
    }

    @ParameterizedTest(name = "{0} a {1} son {2} días")
    @CsvSource({"2030-04-10, 2030-04-16, 7", "2030-04-10, 2030-04-24, 15", "2030-02-27, 2030-03-05, 7", "2030-04-01, 2030-04-30, 30"})
    void losDiasSonCalendarioContandoAmbosExtremos(String desde, String hasta, int dias) {
        LocalDate d = LocalDate.parse(desde);
        LocalDate h = LocalDate.parse(hasta);
        SolicitudVacaciones s = SolicitudVacaciones.reconstruir(
                UUID.randomUUID(), EMPLEADO, PERIODO, d, h, EstadoSolicitud.SOLICITADA, null);

        assertThat(s.dias()).isEqualTo(dias);
    }

    @Test
    void elRangoDebeSerCoherenteYNoEstarEnElPasado() {
        assertThatThrownBy(() -> solicitud("2030-04-16", "2030-04-10", 30))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("fecha");
        assertThatThrownBy(() -> solicitud("2030-02-28", "2030-03-10", 30))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("pasado");
        assertThatCode(() -> solicitud("2030-03-01", "2030-03-10", 30)).doesNotThrowAnyException();
    }

    @Test
    void nuncaSePideMasDiasQueElSaldoDisponible() {
        assertThatThrownBy(() -> solicitud("2030-04-01", "2030-04-30", 29))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("29");
        assertThatCode(() -> solicitud("2030-04-01", "2030-04-30", 30)).doesNotThrowAnyException();
    }

    @Test
    void elBloqueMinimoEsDeSieteDiasSalvoQueSeTomeTodoElSaldo() {
        assertThatThrownBy(() -> solicitud("2030-04-10", "2030-04-15", 30))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("7");
        assertThatCode(() -> solicitud("2030-04-10", "2030-04-16", 30)).doesNotThrowAnyException();
        assertThatCode(() -> solicitud("2030-04-10", "2030-04-12", 3)).doesNotThrowAnyException();
        assertThatThrownBy(() -> solicitud("2030-04-10", "2030-04-12", 4))
                .isInstanceOf(ReglaDeVacacionesException.class);
    }

    @ParameterizedTest(name = "{0}-{1} contra 10-16 abril: {2}")
    @CsvSource({
        "2030-04-01, 2030-04-09, false",
        "2030-04-01, 2030-04-10, true",
        "2030-04-12, 2030-04-13, true",
        "2030-04-16, 2030-04-20, true",
        "2030-04-17, 2030-04-20, false",
        "2030-04-01, 2030-04-30, true"
    })
    void detectaElSolapeDeRangos(String desde, String hasta, boolean solapa) {
        assertThat(solicitudDeSieteDias().seSolapaCon(LocalDate.parse(desde), LocalDate.parse(hasta)))
                .isEqualTo(solapa);
    }

    @Test
    void elFlujoCompletoDeAprobacionLlegaAFinalizada() {
        SolicitudVacaciones s = solicitudDeSieteDias();

        s.aprobarPorJefe();
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.APROBADA_JEFE);
        s.aprobarPorRrhh();
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.APROBADA_RRHH);
        s.iniciarGoce(LocalDate.of(2030, 4, 10));
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.EN_GOCE);
        s.finalizar(LocalDate.of(2030, 4, 17));
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.FINALIZADA);
    }

    @Test
    void rhhNoPuedeAprobarSinLaAprobacionDelJefe() {
        SolicitudVacaciones s = solicitudDeSieteDias();

        assertThatThrownBy(s::aprobarPorRrhh)
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("SOLICITADA");
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.SOLICITADA);
    }

    @Test
    void elJefeNoPuedeAprobarDosVeces() {
        SolicitudVacaciones s = solicitudDeSieteDias();
        s.aprobarPorJefe();

        assertThatThrownBy(s::aprobarPorJefe).isInstanceOf(ReglaDeVacacionesException.class);
    }

    @Test
    void rechazarGuardaElMotivoYSoloSePuedeAntesDeLaAprobacionDeRrhh() {
        SolicitudVacaciones solicitada = solicitudDeSieteDias();
        solicitada.rechazar("  Cierre de campaña  ");
        assertThat(solicitada.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(solicitada.motivoRechazo()).isEqualTo("Cierre de campaña");

        SolicitudVacaciones aprobadaJefe = en(EstadoSolicitud.APROBADA_JEFE);
        aprobadaJefe.rechazar("Falta de personal");
        assertThat(aprobadaJefe.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);

        assertThatThrownBy(() -> en(EstadoSolicitud.APROBADA_RRHH).rechazar("tarde"))
                .isInstanceOf(ReglaDeVacacionesException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"null", "''", "'   '"}, nullValues = "null")
    void rechazarExigeUnMotivo(String motivo) {
        SolicitudVacaciones s = solicitudDeSieteDias();

        assertThatThrownBy(() -> s.rechazar(motivo))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("motivo");
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.SOLICITADA);
    }

    @Test
    void cancelarSoloSePuedeHastaLaAprobacionDelJefe() {
        SolicitudVacaciones s = solicitudDeSieteDias();
        s.cancelar();
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.CANCELADA);

        SolicitudVacaciones aprobadaJefe = en(EstadoSolicitud.APROBADA_JEFE);
        aprobadaJefe.cancelar();
        assertThat(aprobadaJefe.estado()).isEqualTo(EstadoSolicitud.CANCELADA);

        assertThatThrownBy(() -> en(EstadoSolicitud.APROBADA_RRHH).cancelar())
                .isInstanceOf(ReglaDeVacacionesException.class);
        assertThatThrownBy(() -> en(EstadoSolicitud.EN_GOCE).cancelar())
                .isInstanceOf(ReglaDeVacacionesException.class);
        assertThatThrownBy(() -> en(EstadoSolicitud.CANCELADA).cancelar())
                .isInstanceOf(ReglaDeVacacionesException.class);
    }

    @Test
    void elGoceEmpiezaDesdeLaFechaDeInicio() {
        SolicitudVacaciones s = en(EstadoSolicitud.APROBADA_RRHH);

        assertThatThrownBy(() -> s.iniciarGoce(LocalDate.of(2030, 4, 9)))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("2030-04-10");
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.APROBADA_RRHH);
        s.iniciarGoce(LocalDate.of(2030, 4, 10));
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.EN_GOCE);
    }

    @Test
    void soloSeIniciaElGoceSiRrhhYaAprobo() {
        assertThatThrownBy(() -> en(EstadoSolicitud.APROBADA_JEFE).iniciarGoce(LocalDate.of(2030, 4, 10)))
                .isInstanceOf(ReglaDeVacacionesException.class);
    }

    @Test
    void elGocePuedeFinalizarSoloDespuesDelUltimoDia() {
        SolicitudVacaciones s = en(EstadoSolicitud.EN_GOCE);

        assertThatThrownBy(() -> s.finalizar(LocalDate.of(2030, 4, 16)))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("2030-04-16");
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.EN_GOCE);
        s.finalizar(LocalDate.of(2030, 4, 17));
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.FINALIZADA);
    }

    @Test
    void soloFinalizaUnaSolicitudEnGoce() {
        assertThatThrownBy(() -> en(EstadoSolicitud.APROBADA_RRHH).finalizar(LocalDate.of(2031, 1, 1)))
                .isInstanceOf(ReglaDeVacacionesException.class);
    }

    @Test
    void reconstruirConservaTodosLosDatos() {
        UUID id = UUID.randomUUID();

        SolicitudVacaciones s = SolicitudVacaciones.reconstruir(
                id, EMPLEADO, PERIODO, LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 10), EstadoSolicitud.RECHAZADA, "Motivo X");

        assertThat(s.id()).isEqualTo(id);
        assertThat(s.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(s.motivoRechazo()).isEqualTo("Motivo X");
        assertThat(s.dias()).isEqualTo(10);
    }
}
