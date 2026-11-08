package pe.nexosoftware.planilla360.asistencia.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion.ENTRADA;
import static pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion.SALIDA;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.asistencia.domain.excepcion.ReglaDeAsistenciaException;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

class PoliticaMarcacionTests {

    private static final Instant BASE = Instant.parse("2026-03-10T13:00:00Z");

    private final PoliticaMarcacion politica = new PoliticaMarcacion();

    private static Marcacion marca(TipoMarcacion tipo, long segundosDespuesDeBase) {
        return new Marcacion(BASE.plusSeconds(segundosDespuesDeBase), tipo);
    }

    @Test
    void laPrimeraMarcacionDelDiaEsEntrada() {
        assertThat(politica.siguienteTipo(List.of(), BASE)).isEqualTo(ENTRADA);
    }

    @Test
    void despuesDeUnaEntradaSigueUnaSalida() {
        assertThat(politica.siguienteTipo(List.of(marca(ENTRADA, 0)), BASE.plusSeconds(4 * 3600)))
                .isEqualTo(SALIDA);
    }

    @Test
    void despuesDeUnaSalidaSigueOtraEntrada() {
        List<Marcacion> previas = List.of(marca(ENTRADA, 0), marca(SALIDA, 4 * 3600));

        assertThat(politica.siguienteTipo(previas, BASE.plusSeconds(5 * 3600))).isEqualTo(ENTRADA);
    }

    @Test
    void seBasaEnLaUltimaMarcacionAunqueVenganDesordenadas() {
        List<Marcacion> previas = List.of(marca(SALIDA, 4 * 3600), marca(ENTRADA, 0));

        assertThat(politica.siguienteTipo(previas, BASE.plusSeconds(5 * 3600))).isEqualTo(ENTRADA);
    }

    @Test
    void rechazaUnaMarcacionAMenosDeUnMinutoDeLaAnterior() {
        List<Marcacion> previas = List.of(marca(ENTRADA, 0));

        assertThatThrownBy(() -> politica.siguienteTipo(previas, BASE.plusSeconds(59)))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("minuto");
        assertThatThrownBy(() -> politica.siguienteTipo(previas, BASE))
                .isInstanceOf(ReglaDeAsistenciaException.class);
        assertThat(politica.siguienteTipo(previas, BASE.plusSeconds(60))).isEqualTo(SALIDA);
    }

    @Test
    void rechazaUnaMarcacionAnteriorALaUltimaRegistrada() {
        List<Marcacion> previas = List.of(marca(ENTRADA, 3600));

        assertThatThrownBy(() -> politica.siguienteTipo(previas, BASE))
                .isInstanceOf(ReglaDeAsistenciaException.class);
    }

    @Test
    void ponePorTopeOchoMarcacionesPorDia() {
        List<Marcacion> previas = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            previas.add(marca(i % 2 == 0 ? ENTRADA : SALIDA, i * 600L));
        }
        Instant ahora = BASE.plusSeconds(7 * 600L);
        assertThat(politica.siguienteTipo(previas, ahora)).isEqualTo(SALIDA);

        previas.add(marca(SALIDA, 7 * 600L));
        assertThatThrownBy(() -> politica.siguienteTipo(previas, BASE.plusSeconds(8 * 600L)))
                .isInstanceOf(ReglaDeAsistenciaException.class)
                .hasMessageContaining("8");
    }
}
