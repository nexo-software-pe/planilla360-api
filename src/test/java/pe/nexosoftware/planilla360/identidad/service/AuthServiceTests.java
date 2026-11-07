package pe.nexosoftware.planilla360.identidad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.CuentaBloqueadaException;
import pe.nexosoftware.planilla360.compartido.excepcion.NoAutenticadoException;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;
import pe.nexosoftware.planilla360.identidad.dto.LoginRequest;
import pe.nexosoftware.planilla360.identidad.dto.TokenResponse;
import pe.nexosoftware.planilla360.identidad.dto.UsuarioResponse;
import pe.nexosoftware.planilla360.identidad.entity.RefreshToken;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.mapper.UsuarioMapper;
import pe.nexosoftware.planilla360.identidad.repository.RefreshTokenRepository;
import pe.nexosoftware.planilla360.identidad.repository.UsuarioRepository;

class AuthServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-07T12:00:00Z");
    private static final String CLAVE_PRUEBA = "Clave-Segura-123";

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final TokenService tokens = mock(TokenService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final LoginRateLimiter limiter = mock(LoginRateLimiter.class);
    private final EmpresaApi empresas = mock(EmpresaApi.class);
    private final UsuarioMapper mapper = mock(UsuarioMapper.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private AuthService service;
    private UUID tenant;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new AuthService(usuarios, refreshTokens, tokens, encoder, limiter, empresas, mapper, clock);
        tenant = UUID.randomUUID();
        usuario = new Usuario(tenant, "rrhh@ejemplo.pe", "RRHH", encoder.encode(CLAVE_PRUEBA), Rol.RRHH, null);
        when(usuarios.findByEmailIgnoreCase("rrhh@ejemplo.pe")).thenReturn(Optional.of(usuario));
        when(empresas.buscarPorId(tenant)).thenReturn(Optional.of(
                new EmpresaResumen(tenant, "20601234565", "Empresa", RegimenLaboral.GENERAL, true)));
        when(tokens.crearAccessToken(any())).thenReturn("jwt");
        when(tokens.crearRefreshToken(any())).thenReturn("refresh");
        when(mapper.aResponse(any())).thenReturn(mock(UsuarioResponse.class));
    }

    @Test
    void loginCorrectoEmiteTokensYRegistraElAcceso() {
        TokenResponse respuesta = service.login(new LoginRequest("rrhh@ejemplo.pe", CLAVE_PRUEBA), "127.0.0.1");

        assertThat(respuesta.accessToken()).isEqualTo("jwt");
        assertThat(respuesta.refreshToken()).isEqualTo("refresh");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(usuario.getUltimoAcceso()).isEqualTo(AHORA);
        verify(limiter).verificar("127.0.0.1", "rrhh@ejemplo.pe");
    }

    @Test
    void passwordIncorrectaCuentaElFalloYNoEmiteTokens() {
        assertThatThrownBy(() -> service.login(new LoginRequest("rrhh@ejemplo.pe", "otra-clave-123"), "127.0.0.1"))
                .isInstanceOf(NoAutenticadoException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        verify(tokens, never()).crearAccessToken(any());
    }

    @Test
    void alQuintoFalloLaCuentaQuedaBloqueadaAunqueLuegoUseLaClaveCorrecta() {
        for (int i = 0; i < AuthService.MAX_INTENTOS_FALLIDOS; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("rrhh@ejemplo.pe", "otra-clave-123"), "ip"))
                    .isInstanceOf(NoAutenticadoException.class);
        }

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        assertThatThrownBy(() -> service.login(new LoginRequest("rrhh@ejemplo.pe", CLAVE_PRUEBA), "ip"))
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    void elBloqueoExpiraConElTiempo() {
        for (int i = 0; i < AuthService.MAX_INTENTOS_FALLIDOS; i++) {
            usuario.registrarFallo(AHORA, AuthService.MAX_INTENTOS_FALLIDOS, AuthService.DURACION_BLOQUEO);
        }
        Clock despues = Clock.fixed(AHORA.plus(AuthService.DURACION_BLOQUEO).plus(Duration.ofSeconds(1)), ZoneOffset.UTC);
        AuthService tardio = new AuthService(usuarios, refreshTokens, tokens, encoder, limiter, empresas, mapper, despues);

        assertThat(tardio.login(new LoginRequest("rrhh@ejemplo.pe", CLAVE_PRUEBA), "ip").accessToken()).isEqualTo("jwt");
    }

    @Test
    void correoInexistenteDaElMismoErrorQueClaveIncorrecta() {
        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@ejemplo.pe", CLAVE_PRUEBA), "ip"))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessage("Credenciales inválidas.");
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() {
        usuario.cambiarEstado(false);
        assertThatThrownBy(() -> service.login(new LoginRequest("rrhh@ejemplo.pe", CLAVE_PRUEBA), "ip"))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void empresaInactivaImpideElLogin() {
        when(empresas.buscarPorId(tenant)).thenReturn(Optional.of(
                new EmpresaResumen(tenant, "20601234565", "Empresa", RegimenLaboral.GENERAL, false)));
        assertThatThrownBy(() -> service.login(new LoginRequest("rrhh@ejemplo.pe", CLAVE_PRUEBA), "ip"))
                .isInstanceOf(AccesoProhibidoException.class);
    }

    @Test
    void reutilizarUnRefreshTokenRevocadoCierraTodasLasSesiones() {
        RefreshToken usado = new RefreshToken(UUID.randomUUID(), "hash", AHORA.plus(Duration.ofDays(1)), AHORA);
        usado.revocar();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> service.refrescar("token-robado")).isInstanceOf(NoAutenticadoException.class);

        verify(refreshTokens).revocarTodosDelUsuario(usado.getUsuarioId());
    }

    @Test
    void refreshTokenExpiradoSeRechaza() {
        RefreshToken vencido = new RefreshToken(UUID.randomUUID(), "hash", AHORA.minusSeconds(1), AHORA.minusSeconds(60));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> service.refrescar("token")).isInstanceOf(NoAutenticadoException.class);
    }
}
