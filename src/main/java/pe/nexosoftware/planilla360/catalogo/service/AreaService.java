package pe.nexosoftware.planilla360.catalogo.service;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.catalogo.dto.AreaRequest;
import pe.nexosoftware.planilla360.catalogo.dto.AreaResponse;
import pe.nexosoftware.planilla360.catalogo.entity.Area;
import pe.nexosoftware.planilla360.catalogo.mapper.CatalogoMapper;
import pe.nexosoftware.planilla360.catalogo.repository.AreaRepository;
import pe.nexosoftware.planilla360.catalogo.repository.CargoRepository;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private static final String ENTIDAD = "AREA";

    private final AreaRepository areas;
    private final CargoRepository cargos;
    private final CatalogoMapper mapper;
    private final ApplicationEventPublisher eventos;

    public AreaService(
            AreaRepository areas, CargoRepository cargos, CatalogoMapper mapper, ApplicationEventPublisher eventos) {
        this.areas = areas;
        this.cargos = cargos;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    public Pagina<AreaResponse> listar(Pageable pageable) {
        return Pagina.de(areas.findAll(pageable), mapper::aResponse);
    }

    public AreaResponse obtener(UUID id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public AreaResponse crear(AreaRequest request) {
        UUID tenant = TenantContext.requerido();
        if (areas.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new ConflictoException("Ya existe un área con ese nombre.");
        }
        Area area = areas.saveAndFlush(new Area(request.nombre()));
        AreaResponse respuesta = mapper.aResponse(area);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, area.getId(), tenant, Accion.CREAR, null, respuesta));
        return respuesta;
    }

    @Transactional
    public AreaResponse actualizar(UUID id, AreaRequest request) {
        UUID tenant = TenantContext.requerido();
        Area area = buscar(id);
        if (areas.existsByNombreIgnoreCaseAndIdNot(request.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe un área con ese nombre.");
        }
        AreaResponse antes = mapper.aResponse(area);
        area.renombrar(request.nombre());
        areas.flush();
        AreaResponse despues = mapper.aResponse(area);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, tenant, Accion.ACTUALIZAR, antes, despues));
        return despues;
    }

    @Transactional
    public void eliminar(UUID id) {
        UUID tenant = TenantContext.requerido();
        Area area = buscar(id);
        if (cargos.existsByAreaId(id)) {
            throw new ConflictoException("No se puede eliminar un área que tiene cargos asociados.");
        }
        AreaResponse antes = mapper.aResponse(area);
        areas.delete(area);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, tenant, Accion.ELIMINAR, antes, null));
    }

    private Area buscar(UUID id) {
        return areas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el área solicitada."));
    }
}
