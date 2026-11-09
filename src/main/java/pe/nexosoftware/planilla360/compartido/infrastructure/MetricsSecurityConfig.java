package pe.nexosoftware.planilla360.compartido.infrastructure;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protege {@code /actuator/prometheus} con HTTP Basic y una credencial de servicio propia (la que usa Prometheus
 * para raspar). Los JWT de usuarios no sirven aquí: caducan a los 15 minutos y un raspador necesita una credencial
 * estable. Si {@code app.metrics.password} está vacío (staging y prod salvo que se configure) no existe ningún
 * usuario y el endpoint responde siempre 401.
 */
@Configuration
class MetricsSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain metricsFilterChain(
            HttpSecurity http,
            @Value("${app.metrics.user:prometheus}") String usuario,
            @Value("${app.metrics.password:}") String clave)
            throws Exception {
        List<UserDetails> usuarios = clave.isBlank()
                ? List.of()
                : List.of(User.withUsername(usuario).password("{noop}" + clave).roles("METRICS").build());
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(usuarios));
        AuthenticationManager gestor = new ProviderManager(proveedor);
        http.securityMatcher("/actuator/prometheus")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(gestor)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("METRICS"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
