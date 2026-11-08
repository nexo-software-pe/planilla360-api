package pe.nexosoftware.planilla360.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Sustituye el reloj de la aplicación por uno que las pruebas pueden fijar. */
@TestConfiguration(proxyBeanMethods = false)
public class RelojDePruebaConfig {

    @Bean
    @Primary
    RelojDePrueba relojDePrueba() {
        return new RelojDePrueba();
    }
}
