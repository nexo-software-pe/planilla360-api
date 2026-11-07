package pe.nexosoftware.planilla360.compartido.seguridad;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pe.nexosoftware.planilla360.compartido.excepcion.NoAutenticadoException;

/** Usuario que realiza la petición, reconstruido a partir de los claims del JWT. */
public record UsuarioAutenticado(UUID usuarioId, UUID tenantId, Rol rol, String email) {

    public static UsuarioAutenticado desde(Jwt jwt) {
        String tenant = jwt.getClaimAsString(JwtClaims.TENANT);
        return new UsuarioAutenticado(
                UUID.fromString(jwt.getSubject()),
                tenant == null ? null : UUID.fromString(tenant),
                Rol.valueOf(jwt.getClaimAsString(JwtClaims.ROL)),
                jwt.getClaimAsString(JwtClaims.EMAIL));
    }

    public static Optional<UsuarioAutenticado> actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return Optional.of(desde(token.getToken()));
        }
        return Optional.empty();
    }

    public static UsuarioAutenticado requerido() {
        return actual().orElseThrow(NoAutenticadoException::new);
    }

    public boolean esAdminSaas() {
        return rol == Rol.ADMIN_SAAS;
    }
}
