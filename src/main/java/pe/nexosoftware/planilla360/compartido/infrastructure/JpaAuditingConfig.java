package pe.nexosoftware.planilla360.compartido.infrastructure;

import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditingConfig {

    @Bean
    AuditorAware<UUID> auditorProvider() {
        return () -> UsuarioAutenticado.actual().map(UsuarioAutenticado::usuarioId);
    }
}
