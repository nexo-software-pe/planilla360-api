package pe.nexosoftware.planilla360.identidad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.repository.UsuarioRepository;

class AdministradorInicialTests {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private AdministradorInicial con(String email, String password, String nombre) {
        return new AdministradorInicial(new AdministradorInicialProperties(email, password, nombre), usuarios, encoder);
    }

    @Test
    void sinConfigurarNoCreaNada() {
        assertThat(con("", "", null).crearSiHaceFalta()).isFalse();
        assertThat(con(null, null, null).crearSiHaceFalta()).isFalse();
        verify(usuarios, never()).save(any());
    }

    @Test
    void creaElAdministradorDeLaPlataformaConLaClaveCifrada() {
        when(usuarios.existsByEmailIgnoreCase("admin@nexosoftware.pe")).thenReturn(false);

        assertThat(con(" admin@nexosoftware.pe ", "Clave-Inicial-2026", "Ana Admin").crearSiHaceFalta()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getRol()).isEqualTo(Rol.ADMIN_SAAS);
        assertThat(creado.getTenantId()).isNull();
        assertThat(creado.getEmail()).isEqualTo("admin@nexosoftware.pe");
        assertThat(creado.getPasswordHash()).isNotEqualTo("Clave-Inicial-2026");
        assertThat(encoder.matches("Clave-Inicial-2026", creado.getPasswordHash())).isTrue();
    }

    @Test
    void siElCorreoYaExisteNoLoToca() {
        when(usuarios.existsByEmailIgnoreCase("admin@nexosoftware.pe")).thenReturn(true);

        assertThat(con("admin@nexosoftware.pe", "Clave-Inicial-2026", null).crearSiHaceFalta()).isFalse();
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaUnaClaveQueNoCumpleLaPolitica() {
        assertThatThrownBy(() -> con("admin@nexosoftware.pe", "corta", null).crearSiHaceFalta())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("corta");
    }
}
