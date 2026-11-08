package pe.nexosoftware.planilla360.compartido.infrastructure;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Jobs programados con bloqueo distribuido (ShedLock sobre la tabla {@code shedlock}). El bloqueo se
 * pide de forma explícita en cada job con {@link LockingTaskExecutor}, lo que permite probarlo.
 * {@code app.jobs.habilitados=false} apaga el reloj de los {@code @Scheduled} (por ejemplo en pruebas).
 */
@Configuration
class SchedulingConfig {

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
    static class Reloj {
    }

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }

    @Bean
    LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
