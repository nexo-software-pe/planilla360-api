package pe.nexosoftware.planilla360.empresa.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.empresa.dto.EmpresaRequest;
import pe.nexosoftware.planilla360.empresa.dto.EmpresaResponse;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.empresa.mapper.EmpresaMapper;
import pe.nexosoftware.planilla360.empresa.repository.EmpresaRepository;

@Service
@Transactional(readOnly = true)
public class EmpresaService implements EmpresaApi {

    private static final String ENTIDAD = "EMPRESA";

    private final EmpresaRepository repository;
    private final EmpresaMapper mapper;
    private final ApplicationEventPublisher eventos;

    public EmpresaService(EmpresaRepository repository, EmpresaMapper mapper, ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    public Pagina<EmpresaResponse> listar(Boolean activa, Pageable pageable) {
        Page<Empresa> page = activa == null ? repository.findAll(pageable) : repository.findByActiva(activa, pageable);
        return Pagina.de(page, mapper::aResponse);
    }

    public EmpresaResponse obtener(UUID id) {
        return mapper.aResponse(buscar(id));
    }

    @Override
    public Optional<EmpresaResumen> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::aResumen);
    }

    @Transactional
    public EmpresaResponse crear(EmpresaRequest request) {
        if (repository.existsByRuc(request.ruc())) {
            throw new ConflictoException("Ya existe una empresa con el RUC " + request.ruc() + ".");
        }
        Empresa empresa = repository.saveAndFlush(new Empresa(
                request.ruc(), request.razonSocial().trim(), request.regimenLaboral(), request.plan()));
        EmpresaResponse respuesta = mapper.aResponse(empresa);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, empresa.getId(), empresa.getId(), Accion.CREAR, null, respuesta));
        return respuesta;
    }

    @Transactional
    public EmpresaResponse actualizar(UUID id, EmpresaRequest request) {
        Empresa empresa = buscar(id);
        EmpresaResponse antes = mapper.aResponse(empresa);
        if (!empresa.getRuc().equals(request.ruc())) {
            throw new ConflictoException("El RUC de una empresa no puede modificarse.");
        }
        empresa.actualizar(request.razonSocial().trim(), request.regimenLaboral(), request.plan());
        repository.flush();
        EmpresaResponse despues = mapper.aResponse(empresa);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, id, Accion.ACTUALIZAR, antes, despues));
        return despues;
    }

    @Transactional
    public EmpresaResponse cambiarEstado(UUID id, boolean activa) {
        Empresa empresa = buscar(id);
        EmpresaResponse antes = mapper.aResponse(empresa);
        empresa.cambiarEstado(activa);
        repository.flush();
        EmpresaResponse despues = mapper.aResponse(empresa);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, id, Accion.ACTUALIZAR, antes, despues));
        return despues;
    }

    private Empresa buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa solicitada."));
    }
}
