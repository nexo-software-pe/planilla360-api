package pe.nexosoftware.planilla360.auditoria.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.auditoria.dto.AuditoriaResponse;
import pe.nexosoftware.planilla360.auditoria.entity.AuditoriaCambio;
import pe.nexosoftware.planilla360.auditoria.repository.AuditoriaCambioRepository;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final AuditoriaCambioRepository repository;
    private final JsonMapper mapper;

    public AuditoriaService(AuditoriaCambioRepository repository, JsonMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** El administrador de empresa solo ve su empresa; el de la plataforma puede filtrar por empresa o ver todo. */
    public Pagina<AuditoriaResponse> listar(
            String entidad, LocalDate desde, LocalDate hasta, UUID tenantIdFiltro, Pageable pageable) {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        UUID tenant = actor.esAdminSaas() ? tenantIdFiltro : actor.tenantId();

        Specification<AuditoriaCambio> spec = (root, query, cb) -> cb.conjunction();
        if (tenant != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), tenant));
        }
        if (entidad != null && !entidad.isBlank()) {
            String buscada = entidad.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entidad"), buscada));
        }
        if (desde != null) {
            var inicio = desde.atStartOfDay(LIMA).toInstant();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("instante"), inicio));
        }
        if (hasta != null) {
            var fin = hasta.plusDays(1).atStartOfDay(LIMA).toInstant();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("instante"), fin));
        }
        return Pagina.de(repository.findAll(spec, pageable), this::aResponse);
    }

    private AuditoriaResponse aResponse(AuditoriaCambio a) {
        return new AuditoriaResponse(
                a.getId(), a.getTenantId(), a.getEntidad(), a.getEntidadId(), a.getAccion(), a.getUsuario(),
                a.getInstante(), leer(a.getAntes()), leer(a.getDespues()));
    }

    private JsonNode leer(String json) {
        return json == null ? null : mapper.readTree(json);
    }
}
