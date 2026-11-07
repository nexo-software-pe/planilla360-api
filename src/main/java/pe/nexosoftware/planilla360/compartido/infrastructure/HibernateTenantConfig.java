package pe.nexosoftware.planilla360.compartido.infrastructure;

import java.util.UUID;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;

/**
 * Multi-tenancy por columna: Hibernate filtra y asigna {@code tenant_id} en las entidades marcadas con
 * {@code @TenantId}. Sin empresa en contexto se usa un identificador nulo que no coincide con ninguna
 * fila: ante la duda, no se devuelve nada.
 */
@Configuration
class HibernateTenantConfig {

    static final UUID SIN_EMPRESA = new UUID(0L, 0L);

    static class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

        @Override
        public UUID resolveCurrentTenantIdentifier() {
            return TenantContext.actual().orElse(SIN_EMPRESA);
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }
    }

    @Bean
    HibernatePropertiesCustomizer tenantResolverCustomizer() {
        TenantIdentifierResolver resolver = new TenantIdentifierResolver();
        return props -> props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
