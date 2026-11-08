package pe.nexosoftware.planilla360.asistencia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.nexosoftware.planilla360.asistencia.domain.excepcion.ReglaDeAsistenciaException;

class HoraExtraTests {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 10);
    private static final UUID EMPLEADO = UUID.randomUUID();
    private static final UUID JEFE = UUID.randomUUID();

    private static HoraExtra solicitud(String horas, LocalDate fecha) {
        return HoraExtra.solicitar(EMPLEADO, fecha, new BigDecimal(horas), "Cierre de producción", HOY);
    }

    @Test
    void unaSolicitudNuevaQuedaPendienteConSusDatos() {
        HoraExtra horaExtra = solicitud("2.5", HOY);

        assertThat(horaExtra.id()).isNotNull();
        assertThat(horaExtra.empleadoId()).isEqualTo(EMPLEADO);
        assertThat(horaExtra.fecha()).isEqualTo(HOY);
        assertThat(horaExtra.horas()).isEqualByComparingTo("2.5");
        assertThat(horaExtra.motivo()).isEqualTo("Cierre de producción");
        assertThat(horaExtra.estado()).isEqualTo(EstadoHoraExtra.PENDIENTE);
        assertThat(horaExtra.aprobadoPor()).isNull();
    }

    @Test
    void cadaSolicitudTieneSuPropioIdentificador() {
        assertThat(solicitud("1", HOY).id()).isNotEqualTo(solicitud("1", HOY).id());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.1", "0.3", "1.33", "8.25", "9", "12"})
    void lasHorasDebenSerMultiplosDeUnCuartoMayoresACeroYHastaOcho(String horas) {
        assertThatThrownBy(() -> solicitud(horas, HOY)).isInstanceOf(ReglaDeAsistenciaException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.25", "0.5", "1", "2.75", "7.75", "8", "8.00"})
    void lasHorasValidasSeAceptan(String horas) {
        assertThatCode(() -> solicitud(horas, HOY)).doesNotThrowAnyException();
    }

    @Test
    void noSePuedenSolicitarHorasExtraDeUnDiaFuturo() {
        assertThatThrownBy(() -> solicitud("1", HOY.plusDays(1)))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void noSePuedenSolicitarConMasDeTreintaDiasDeRetraso() {
        assertThatCode(() -> solicitud("1", HOY.minusDays(30))).doesNotThrowAnyException();
        assertThatThrownBy(() -> solicitud("1", HOY.minusDays(31)))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("30");
    }

    @Test
    void aprobarRegistraQuienAprueba() {
        HoraExtra horaExtra = solicitud("2", HOY);

        horaExtra.aprobar(JEFE);

        assertThat(horaExtra.estado()).isEqualTo(EstadoHoraExtra.APROBADA);
        assertThat(horaExtra.aprobadoPor()).isEqualTo(JEFE);
    }

    @Test
    void rechazarRegistraQuienRechaza() {
        HoraExtra horaExtra = solicitud("2", HOY);

        horaExtra.rechazar(JEFE);

        assertThat(horaExtra.estado()).isEqualTo(EstadoHoraExtra.RECHAZADA);
        assertThat(horaExtra.aprobadoPor()).isEqualTo(JEFE);
    }

    @Test
    void soloSePuedeResolverUnaVez() {
        HoraExtra aprobada = solicitud("2", HOY);
        aprobada.aprobar(JEFE);
        HoraExtra rechazada = solicitud("2", HOY);
        rechazada.rechazar(JEFE);

        assertThatThrownBy(() -> aprobada.rechazar(JEFE)).isInstanceOf(ReglaDeAsistenciaException.class);
        assertThatThrownBy(() -> aprobada.aprobar(JEFE)).isInstanceOf(ReglaDeAsistenciaException.class);
        assertThatThrownBy(() -> rechazada.aprobar(JEFE)).isInstanceOf(ReglaDeAsistenciaException.class);
        assertThatThrownBy(() -> rechazada.rechazar(JEFE)).isInstanceOf(ReglaDeAsistenciaException.class);
        assertThat(aprobada.estado()).isEqualTo(EstadoHoraExtra.APROBADA);
        assertThat(rechazada.estado()).isEqualTo(EstadoHoraExtra.RECHAZADA);
    }

    @Test
    void resolverRequiereIndicarQuien() {
        HoraExtra horaExtra = solicitud("2", HOY);

        assertThatThrownBy(() -> horaExtra.aprobar(null)).isInstanceOf(ReglaDeAsistenciaException.class);
        assertThatThrownBy(() -> horaExtra.rechazar(null)).isInstanceOf(ReglaDeAsistenciaException.class);
        assertThat(horaExtra.estado()).isEqualTo(EstadoHoraExtra.PENDIENTE);
    }

    @Test
    void reconstruirConservaTodosLosDatosGuardados() {
        UUID id = UUID.randomUUID();

        HoraExtra horaExtra = HoraExtra.reconstruir(
                id, EMPLEADO, HOY, new BigDecimal("3"), "Inventario", EstadoHoraExtra.APROBADA, JEFE);

        assertThat(horaExtra.id()).isEqualTo(id);
        assertThat(horaExtra.estado()).isEqualTo(EstadoHoraExtra.APROBADA);
        assertThat(horaExtra.aprobadoPor()).isEqualTo(JEFE);
        assertThat(horaExtra.motivo()).isEqualTo("Inventario");
    }
}
