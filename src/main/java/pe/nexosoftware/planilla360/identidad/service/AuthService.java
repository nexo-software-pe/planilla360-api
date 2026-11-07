package pe.nexosoftware.planilla360.identidad.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.CuentaBloqueadaException;
import pe.nexosoftware.planilla360.compartido.excepcion.NoAutenticadoException;
import pe.nexosoftware.planilla360.compartido.excepcion.PlanillaException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.identidad.dto.CambiarPasswordRequest;
import pe.nexosoftware.planilla360.identidad.dto.LoginRequest;
import pe.nexosoftware.planilla360.identidad.dto.PerfilResponse;
import pe.nexosoftware.planilla360.identidad.dto.TokenResponse;
import pe.nexosoftware.planilla360.identidad.entity.RefreshToken;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.mapper.UsuarioMapper;
import pe.nexosoftware.planilla360.identidad.repository.RefreshTokenRepository;
import pe.nexosoftware.planilla360.identidad.repository.UsuarioRepository;

@Service
public class AuthService {

    static final int MAX_INTENTOS_FALLIDOS = 5;
    static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);
    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas.";

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder encoder;
    private final LoginRateLimiter limiter;
    private final EmpresaApi empresas;
    private final UsuarioMapper mapper;
    private final Clock clock;
    /** Hash de relleno: se verifica contra él cuando el correo no existe, para no revelar su existencia por tiempo. */
    private final String hashRelleno;

    public AuthService(
            UsuarioRepository usuarios,
            RefreshTokenRepository refreshTokens,
            TokenService tokens,
            PasswordEncoder encoder,
            LoginRateLimiter limiter,
            EmpresaApi empresas,
            UsuarioMapper mapper,
            Clock clock) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.encoder = encoder;
        this.limiter = limiter;
        this.empresas = empresas;
        this.mapper = mapper;
        this.clock = clock;
        this.hashRelleno = encoder.encode(UUID.randomUUID().toString());
    }

    /** Los fallos se persisten (contador y bloqueo) aunque la operación termine en error. */
    @Transactional(noRollbackFor = PlanillaException.class)
    public TokenResponse login(LoginRequest request, String ipCliente) {
        limiter.verificar(ipCliente, request.email());
        Instant ahora = clock.instant();
        Usuario usuario = usuarios.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (usuario == null) {
            encoder.matches(request.password(), hashRelleno);
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        if (usuario.estaBloqueado(ahora)) {
            throw new CuentaBloqueadaException(
                    "La cuenta está bloqueada por intentos fallidos. Intenta nuevamente más tarde.");
        }
        if (!encoder.matches(request.password(), usuario.getPasswordHash())) {
            usuario.registrarFallo(ahora, MAX_INTENTOS_FALLIDOS, DURACION_BLOQUEO);
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        validarCuentaOperativa(usuario);
        usuario.registrarAcceso(ahora);
        return emitir(usuario);
    }

    /** Rotación: cada refresh token se usa una vez. Reutilizar uno ya usado revoca toda la sesión. */
    @Transactional(noRollbackFor = PlanillaException.class)
    public TokenResponse refrescar(String refreshTokenEnClaro) {
        RefreshToken token = refreshTokens.findByTokenHash(TokenService.hash(refreshTokenEnClaro))
                .orElseThrow(() -> new NoAutenticadoException("Sesión inválida."));
        if (token.isRevocado()) {
            refreshTokens.revocarTodosDelUsuario(token.getUsuarioId());
            throw new NoAutenticadoException("Sesión inválida.");
        }
        if (!token.estaVigente(clock.instant())) {
            throw new NoAutenticadoException("La sesión expiró. Inicia sesión nuevamente.");
        }
        Usuario usuario = usuarios.findById(token.getUsuarioId())
                .orElseThrow(() -> new NoAutenticadoException("Sesión inválida."));
        validarCuentaOperativa(usuario);
        token.revocar();
        return emitir(usuario);
    }

    @Transactional
    public void cerrarSesion(String refreshTokenEnClaro) {
        refreshTokens.findByTokenHash(TokenService.hash(refreshTokenEnClaro)).ifPresent(RefreshToken::revocar);
    }

    @Transactional(readOnly = true)
    public PerfilResponse perfil() {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        Usuario usuario = usuarios.findById(actor.usuarioId()).orElseThrow(NoAutenticadoException::new);
        EmpresaResumen empresa = usuario.getTenantId() == null
                ? null
                : empresas.buscarPorId(usuario.getTenantId()).orElse(null);
        return new PerfilResponse(mapper.aResponse(usuario), empresa);
    }

    @Transactional(noRollbackFor = PlanillaException.class)
    public void cambiarPassword(CambiarPasswordRequest request) {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        Usuario usuario = usuarios.findById(actor.usuarioId()).orElseThrow(NoAutenticadoException::new);
        if (!encoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new ReglaDeNegocioException("La contraseña actual no es correcta.");
        }
        usuario.cambiarPassword(encoder.encode(request.passwordNueva()));
        refreshTokens.revocarTodosDelUsuario(usuario.getId());
    }

    private void validarCuentaOperativa(Usuario usuario) {
        if (!usuario.isActivo()) {
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        if (usuario.getTenantId() != null) {
            boolean empresaActiva =
                    empresas.buscarPorId(usuario.getTenantId()).map(EmpresaResumen::activa).orElse(false);
            if (!empresaActiva) {
                throw new AccesoProhibidoException("La empresa está inactiva. Contacta al administrador.");
            }
        }
    }

    private TokenResponse emitir(Usuario usuario) {
        return new TokenResponse(
                tokens.crearAccessToken(usuario),
                tokens.crearRefreshToken(usuario),
                "Bearer",
                tokens.segundosDeVidaAccessToken(),
                mapper.aResponse(usuario));
    }
}
