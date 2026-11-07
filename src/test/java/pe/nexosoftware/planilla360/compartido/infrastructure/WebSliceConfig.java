package pe.nexosoftware.planilla360.compartido.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Configuración de seguridad real para las pruebas {@code @WebMvcTest}. */
@TestConfiguration(proxyBeanMethods = false)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class, ProblemDetailSecurityHandler.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class WebSliceConfig {
}
