package pe.nexosoftware.planilla360.catalogo.service;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.catalogo.dto.CargoRequest;
import pe.nexosoftware.planilla360.catalogo.dto.CargoResponse;
import pe.nexosoftware.planilla360.catalogo.entity.Cargo;
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
public class CargoService {

    private static final String ENTIDAD = "CARGO";

    private final CargoRepository cargos;
    private final AreaRepository areas;
    private final CatalogoMapper mapper;
    private final ApplicationEventPublisher eventos;

    public CargoService(
            CargoRepository cargos, AreaRepository areas, CatalogoMapper mapper, ApplicationEventPublisher eventos) {
        this.cargos = cargos;
        this.areas = areas;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    public Pagina<CargoResponse> listar(UUID areaId, Pageable pageable) {
        Page<Cargo> page = areaId == null ? cargos.findAll(pageable) : cargos.findByAreaId(areaId, pageable);
        return Pagina.de(page, mapper::aResponse);
    }

    public CargoResponse obtener(UUID id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public CargoResponse crear(CargoRequest request) {
        UUID tenant = TenantContext.requerido();
        verificarArea(request.areaId());
        if (cargos.existsByAreaIdAndNombreIgnoreCase(request.areaId(), request.nombre().trim())) {
            throw new ConflictoException("Ya existe un cargo con ese nombre en el área.");
        }
        Cargo cargo = cargos.saveAndFlush(new Cargo(request.areaId(), request.nombre(), request.sueldoReferencial()));
        CargoResponse respuesta = mapper.aResponse(cargo);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, cargo.getId(), tenant, Accion.CREAR, null, respuesta));
        return respuesta;
    }

    @Transactional
    public CargoResponse actualizar(UUID id, CargoRequest request) {
        UUID tenant = TenantContext.requerido();
        Cargo cargo = buscar(id);
        verificarArea(request.areaId());
        if (cargos.existsByAreaIdAndNombreIgnoreCaseAndIdNot(request.areaId(), request.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe un cargo con ese nombre en el área.");
        }
        CargoResponse antes = mapper.aResponse(cargo);
        cargo.actualizar(request.areaId(), request.nombre(), request.sueldoReferencial());
        cargos.flush();
        CargoResponse despues = mapper.aResponse(cargo);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, tenant, Accion.ACTUALIZAR, antes, despues));
        return despues;
    }

    @Transactional
    public void eliminar(UUID id) {
        UUID tenant = TenantContext.requerido();
        Cargo cargo = buscar(id);
        CargoResponse antes = mapper.aResponse(cargo);
        cargos.delete(cargo);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, tenant, Accion.ELIMINAR, antes, null));
    }

    private void verificarArea(UUID areaId) {
        if (areas.findById(areaId).isEmpty()) {
            throw new RecursoNoEncontradoException("No existe el área indicada.");
        }
    }

    private Cargo buscar(UUID id) {
        return cargos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el cargo solicitado."));
    }
}
