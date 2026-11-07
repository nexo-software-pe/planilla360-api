package pe.nexosoftware.planilla360.compartido.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;

/**
 * Puebla el {@link TenantContext} con la empresa del JWT ya validado y lo limpia al terminar, para que
 * el hilo del servidor nunca arrastre la empresa de una petición anterior.
 */
class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            UsuarioAutenticado.actual()
                    .map(UsuarioAutenticado::tenantId)
                    .ifPresent((UUID tenantId) -> TenantContext.establecer(tenantId));
            chain.doFilter(request, response);
        } finally {
            TenantContext.limpiar();
        }
    }
}
