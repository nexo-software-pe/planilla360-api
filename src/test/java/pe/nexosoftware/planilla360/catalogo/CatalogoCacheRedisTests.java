package pe.nexosoftware.planilla360.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mockingDetails;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import pe.nexosoftware.planilla360.catalogo.repository.AfpRepository;
import pe.nexosoftware.planilla360.catalogo.service.CatalogoCache;
import pe.nexosoftware.planilla360.soporte.TestcontainersConfig;

/** Con Redis real, el segundo acceso a un catálogo no vuelve a consultar la base de datos. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.cache.type=redis")
@Import({TestcontainersConfig.class, CatalogoCacheRedisTests.RedisConfig.class})
class CatalogoCacheRedisTests {

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisConfig {

        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redis() {
            return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CatalogoCache cache;

    @Autowired
    private StringRedisTemplate redis;

    @MockitoSpyBean
    private AfpRepository afps;

    @Test
    void elGestorDeCacheEsRedis() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void lasAfpSeLeenUnaSolaVezDeLaBaseDeDatosYQuedanEnRedis() {
        var primera = cache.afps();
        // La escritura en Redis puede completarse un instante después de devolver la primera lectura.
        await().atMost(Duration.ofSeconds(5)).until(() -> !redis.keys(CatalogoCache.AFPS + "*").isEmpty());

        var segunda = cache.afps();
        var tercera = cache.afps();

        assertThat(segunda).isEqualTo(primera);
        assertThat(tercera).isEqualTo(primera);
        assertThat(mockingDetails(afps).getInvocations()).hasSize(1);
    }
}
