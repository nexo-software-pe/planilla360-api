package pe.nexosoftware.planilla360.identidad.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

class UsuarioTests {

    private static final Instant AHORA = Instant.parse("2026-11-07T12:00:00Z");

    private Usuario usuario() {
        return new Usuario(UUID.randomUUID(), "  Rosa@Ejemplo.PE ", " Rosa ", "hash", Rol.RRHH, null);
    }

    @Test
    void normalizaCorreoYNombre() {
        Usuario u = usuario();
        assertThat(u.getEmail()).isEqualTo("rosa@ejemplo.pe");
        assertThat(u.getNombre()).isEqualTo("Rosa");
    }

    @Test
    void sebloqueaAlLlegarAlMaximoDeIntentos() {
        Usuario u = usuario();
        for (int i = 0; i < 4; i++) {
            u.registrarFallo(AHORA, 5, Duration.ofMinutes(15));
        }
        assertThat(u.estaBloqueado(AHORA)).isFalse();
        u.registrarFallo(AHORA, 5, Duration.ofMinutes(15));
        assertThat(u.estaBloqueado(AHORA)).isTrue();
        assertThat(u.estaBloqueado(AHORA.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    void unAccesoExitosoReiniciaElContador() {
        Usuario u = usuario();
        u.registrarFallo(AHORA, 5, Duration.ofMinutes(15));
        u.registrarAcceso(AHORA);
        assertThat(u.getIntentosFallidos()).isZero();
        assertThat(u.getUltimoAcceso()).isEqualTo(AHORA);
    }

    @Test
    void reactivarOCambiarPasswordDesbloquea() {
        Usuario u = usuario();
        for (int i = 0; i < 5; i++) {
            u.registrarFallo(AHORA, 5, Duration.ofMinutes(15));
        }
        u.cambiarPassword("otro-hash");
        assertThat(u.estaBloqueado(AHORA)).isFalse();
        assertThat(u.getPasswordHash()).isEqualTo("otro-hash");
    }
}
