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

class PermisoTests {

    private static final UUID EMPLEADO = UUID.randomUUID();
    private static final LocalDate DIA = LocalDate.of(2030, 4, 10);

    private static Permiso permiso(LocalDate desde, LocalDate hasta) {
        return Permiso.solicitar(EMPLEADO, TipoPermiso.MEDICO, desde, hasta, true, "Cita médica");
    }

    @Test
    void unPermisoNuevoQuedaSolicitadoConSusDatos() {
        Permiso p = Permiso.solicitar(EMPLEADO, TipoPermiso.LICENCIA, DIA, DIA.plusDays(2), false, "Mudanza");

        assertThat(p.id()).isNotNull();
        assertThat(p.empleadoId()).isEqualTo(EMPLEADO);
        assertThat(p.tipo()).isEqualTo(TipoPermiso.LICENCIA);
        assertThat(p.desde()).isEqualTo(DIA);
        assertThat(p.hasta()).isEqualTo(DIA.plusDays(2));
        assertThat(p.conGoce()).isFalse();
        assertThat(p.motivo()).isEqualTo("Mudanza");
        assertThat(p.estado()).isEqualTo(EstadoPermiso.SOLICITADO);
        assertThat(p.motivoRechazo()).isNull();
        assertThat(p.dias()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"2030-04-10, 2030-04-10, 1", "2030-04-10, 2030-04-11, 2", "2030-04-30, 2030-05-02, 3"})
    void losDiasIncluyenAmbosExtremos(String desde, String hasta, int dias) {
        assertThat(permiso(LocalDate.parse(desde), LocalDate.parse(hasta)).dias()).isEqualTo(dias);
    }

    @Test
    void elRangoNoPuedeEstarInvertido() {
        assertThatThrownBy(() -> permiso(DIA, DIA.minusDays(1)))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("fecha");
        assertThatCode(() -> permiso(DIA, DIA)).doesNotThrowAnyException();
    }

    @Test
    void nuncaDuraMasDeNoventaDias() {
        assertThatCode(() -> permiso(DIA, DIA.plusDays(89))).doesNotThrowAnyException();
        assertThatThrownBy(() -> permiso(DIA, DIA.plusDays(90)))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("90");
    }

    @Test
    void aprobarCambiaElEstadoUnaSolaVez() {
        Permiso p = permiso(DIA, DIA);

        p.aprobar();

        assertThat(p.estado()).isEqualTo(EstadoPermiso.APROBADO);
        assertThatThrownBy(p::aprobar).isInstanceOf(ReglaDeVacacionesException.class);
        assertThatThrownBy(() -> p.rechazar("tarde")).isInstanceOf(ReglaDeVacacionesException.class);
    }

    @Test
    void rechazarGuardaElMotivoSinEspaciosSobrantes() {
        Permiso p = permiso(DIA, DIA);

        p.rechazar("  Sin sustento  ");

        assertThat(p.estado()).isEqualTo(EstadoPermiso.RECHAZADO);
        assertThat(p.motivoRechazo()).isEqualTo("Sin sustento");
        assertThatThrownBy(p::aprobar).isInstanceOf(ReglaDeVacacionesException.class);
        assertThatThrownBy(() -> p.rechazar("otra vez")).isInstanceOf(ReglaDeVacacionesException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"null", "''", "'  '"}, nullValues = "null")
    void rechazarExigeUnMotivo(String motivo) {
        Permiso p = permiso(DIA, DIA);

        assertThatThrownBy(() -> p.rechazar(motivo))
                .isInstanceOf(ReglaDeVacacionesException.class)
                .hasMessageContaining("motivo");
        assertThat(p.estado()).isEqualTo(EstadoPermiso.SOLICITADO);
    }

    @Test
    void reconstruirConservaTodosLosDatos() {
        UUID id = UUID.randomUUID();

        Permiso p = Permiso.reconstruir(
                id, EMPLEADO, TipoPermiso.PERSONAL, DIA, DIA, false, "Trámite", EstadoPermiso.RECHAZADO, "No procede");

        assertThat(p.id()).isEqualTo(id);
        assertThat(p.estado()).isEqualTo(EstadoPermiso.RECHAZADO);
        assertThat(p.motivoRechazo()).isEqualTo("No procede");
        assertThat(p.conGoce()).isFalse();
    }
}
