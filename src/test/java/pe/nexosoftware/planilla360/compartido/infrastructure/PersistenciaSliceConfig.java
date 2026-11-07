package pe.nexosoftware.planilla360.compartido.infrastructure;

import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Configuración de persistencia real (tenant + auditoría JPA) para las pruebas {@code @DataJpaTest}. */
@TestConfiguration(proxyBeanMethods = false)
@Import({HibernateTenantConfig.class, JpaAuditingConfig.class})
public class PersistenciaSliceConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
