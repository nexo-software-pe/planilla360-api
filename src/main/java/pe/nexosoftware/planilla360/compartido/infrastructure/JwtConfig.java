package pe.nexosoftware.planilla360.compartido.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import pe.nexosoftware.planilla360.compartido.seguridad.JwtClaims;

@Configuration
class JwtConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecretKey jwtSecretKey(JwtProperties props) {
        byte[] bytes;
        if (props.secret() == null || props.secret().isBlank()) {
            if (!props.generarSiFalta()) {
                throw new IllegalStateException(
                        "Falta la variable de entorno JWT_SECRET (mínimo 32 caracteres).");
            }
            bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            LOG.warn("JWT_SECRET no definido: se generó un secreto efímero. Los tokens dejan de ser válidos al reiniciar.");
        } else {
            bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties props) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString(JwtClaims.ROL);
            return rol == null
                    ? List.<GrantedAuthority>of()
                    : List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + rol));
        });
        return converter;
    }
}
