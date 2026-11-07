package pe.nexosoftware.planilla360.compartido.tenant;

import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

/**
 * Empresa (tenant) de la petición en curso. Se puebla desde el JWT en un filtro y se limpia al terminar
 * la petición. Hibernate lo consulta para filtrar y asignar el {@code tenant_id} de las entidades.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> ACTUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void establecer(UUID tenantId) {
        ACTUAL.set(tenantId);
    }

    public static Optional<UUID> actual() {
        return Optional.ofNullable(ACTUAL.get());
    }

    public static UUID requerido() {
        return actual().orElseThrow(
                () -> new ReglaDeNegocioException("Esta operación requiere una empresa en contexto."));
    }

    public static void limpiar() {
        ACTUAL.remove();
    }
}
