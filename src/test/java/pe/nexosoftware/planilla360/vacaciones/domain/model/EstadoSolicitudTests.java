package pe.nexosoftware.planilla360.vacaciones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.APROBADA_JEFE;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.APROBADA_RRHH;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.CANCELADA;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.EN_GOCE;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.FINALIZADA;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.RECHAZADA;
import static pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud.SOLICITADA;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EstadoSolicitudTests {

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> PERMITIDAS = Map.of(
            SOLICITADA, EnumSet.of(APROBADA_JEFE, RECHAZADA, CANCELADA),
            APROBADA_JEFE, EnumSet.of(APROBADA_RRHH, RECHAZADA, CANCELADA),
            APROBADA_RRHH, EnumSet.of(EN_GOCE),
            EN_GOCE, EnumSet.of(FINALIZADA),
            FINALIZADA, EnumSet.noneOf(EstadoSolicitud.class),
            RECHAZADA, EnumSet.noneOf(EstadoSolicitud.class),
            CANCELADA, EnumSet.noneOf(EstadoSolicitud.class));

    @ParameterizedTest
    @EnumSource(EstadoSolicitud.class)
    void soloSePuedePasarALosEstadosPrevistosPorElFlujo(EstadoSolicitud origen) {
        for (EstadoSolicitud destino : EstadoSolicitud.values()) {
            assertThat(origen.puedePasarA(destino))
                    .as("%s -> %s", origen, destino)
                    .isEqualTo(PERMITIDAS.get(origen).contains(destino));
        }
    }

    @Test
    void soloLasSolicitudesActivasReservanDias() {
        assertThat(EnumSet.allOf(EstadoSolicitud.class).stream().filter(EstadoSolicitud::reservaDias))
                .containsExactlyInAnyOrder(SOLICITADA, APROBADA_JEFE, APROBADA_RRHH, EN_GOCE);
    }

    @Test
    void finalizadaRechazadaYCanceladaSonTerminales() {
        assertThat(EnumSet.allOf(EstadoSolicitud.class).stream().filter(EstadoSolicitud::esTerminal))
                .containsExactlyInAnyOrder(FINALIZADA, RECHAZADA, CANCELADA);
    }
}
