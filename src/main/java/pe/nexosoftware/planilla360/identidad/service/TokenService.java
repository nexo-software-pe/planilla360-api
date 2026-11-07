package pe.nexosoftware.planilla360.identidad.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pe.nexosoftware.planilla360.compartido.infrastructure.JwtProperties;
import pe.nexosoftware.planilla360.compartido.seguridad.JwtClaims;
import pe.nexosoftware.planilla360.identidad.entity.RefreshToken;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.repository.RefreshTokenRepository;

/** Emisión de access tokens (JWT firmado) y refresh tokens (opacos, guardados con hash). */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public TokenService(
            JwtEncoder encoder, JwtProperties props, RefreshTokenRepository refreshTokens, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    public String crearAccessToken(Usuario usuario) {
        Instant ahora = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(props.accessTtl()))
                .claim(JwtClaims.ROL, usuario.getRol().name())
                .claim(JwtClaims.EMAIL, usuario.getEmail());
        if (usuario.getTenantId() != null) {
            claims.claim(JwtClaims.TENANT, usuario.getTenantId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    /** Crea y guarda un refresh token; devuelve el valor en claro (única vez que existe). */
    public String crearRefreshToken(Usuario usuario) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ahora = clock.instant();
        refreshTokens.save(new RefreshToken(usuario.getId(), hash(valor), ahora.plus(props.refreshTtl()), ahora));
        return valor;
    }

    public long segundosDeVidaAccessToken() {
        return props.accessTtl().toSeconds();
    }

    public static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
