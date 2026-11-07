package pe.nexosoftware.planilla360.compartido.infrastructure;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Caché de catálogos y parámetros legales (Redis en entornos con Redis; en memoria si no). */
@Configuration
@EnableCaching
class CacheConfig {
}
