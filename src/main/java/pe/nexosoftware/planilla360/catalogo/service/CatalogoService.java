package pe.nexosoftware.planilla360.catalogo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.catalogo.AfpResumen;
import pe.nexosoftware.planilla360.catalogo.CatalogoApi;
import pe.nexosoftware.planilla360.catalogo.dto.AfpResponse;
import pe.nexosoftware.planilla360.catalogo.dto.FeriadoResponse;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalRequest;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalResponse;
import pe.nexosoftware.planilla360.catalogo.entity.ParametroLegal;
import pe.nexosoftware.planilla360.catalogo.mapper.CatalogoMapper;
import pe.nexosoftware.planilla360.catalogo.repository.AfpRepository;
import pe.nexosoftware.planilla360.catalogo.repository.AreaRepository;
import pe.nexosoftware.planilla360.catalogo.repository.CargoRepository;
import pe.nexosoftware.planilla360.catalogo.repository.FeriadoRepository;
import pe.nexosoftware.planilla360.catalogo.repository.ParametroLegalRepository;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

/** Catálogos globales: AFP, parámetros legales (cacheados) y feriados. */
@Service
@Transactional(readOnly = true)
public class CatalogoService implements CatalogoApi {

    private final CatalogoCache cache;
    private final ParametroLegalRepository parametros;
    private final FeriadoRepository feriados;
    private final AreaRepository areas;
    private final CargoRepository cargos;
    private final AfpRepository afpRepository;
    private final CatalogoMapper mapper;
    private final ApplicationEventPublisher eventos;

    public CatalogoService(
            CatalogoCache cache,
            ParametroLegalRepository parametros,
            FeriadoRepository feriados,
            AreaRepository areas,
            CargoRepository cargos,
            AfpRepository afpRepository,
            CatalogoMapper mapper,
            ApplicationEventPublisher eventos) {
        this.cache = cache;
        this.parametros = parametros;
        this.feriados = feriados;
        this.areas = areas;
        this.cargos = cargos;
        this.afpRepository = afpRepository;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    @Override
    public BigDecimal valorParametro(String codigo, LocalDate fecha) {
        return parametroVigente(codigo, fecha).orElseThrow(() -> new RecursoNoEncontradoException(
                "No hay un valor vigente del parámetro " + codigo + " para " + fecha + "."));
    }

    @Override
    public Optional<BigDecimal> parametroVigente(String codigo, LocalDate fecha) {
        return cache.parametros().stream()
                .filter(p -> p.codigo().equals(codigo))
                .filter(p -> !p.vigenteDesde().isAfter(fecha))
                .filter(p -> p.vigenteHasta() == null || !p.vigenteHasta().isBefore(fecha))
                .max(Comparator.comparing(ParametroLegalResponse::vigenteDesde))
                .map(ParametroLegalResponse::valor);
    }

    @Override
    public List<AfpResumen> afps() {
        return cache.afps().stream().map(mapper::aResumen).toList();
    }

    @Override
    public boolean existeArea(UUID id) {
        return areas.existsById(id);
    }

    @Override
    public boolean existeCargo(UUID id) {
        return cargos.existsById(id);
    }

    @Override
    public boolean existeAfp(UUID id) {
        return afpRepository.existsById(id);
    }

    @Override
    public boolean esFeriado(LocalDate fecha) {
        return !feriados.findByFechaBetweenOrderByFechaAsc(fecha, fecha).isEmpty();
    }

    public List<AfpResponse> listarAfps() {
        return cache.afps();
    }

    public List<ParametroLegalResponse> listarParametros(String codigo) {
        return cache.parametros().stream().filter(p -> codigo == null || p.codigo().equals(codigo)).toList();
    }

    public List<FeriadoResponse> listarFeriados(int anio) {
        return feriados.findByFechaBetweenOrderByFechaAsc(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31))
                .stream()
                .map(mapper::aResponse)
                .toList();
    }

    /** Registra una nueva vigencia del parámetro y cierra la anterior el día previo. */
    @Transactional
    @CacheEvict(cacheNames = CatalogoCache.PARAMETROS, allEntries = true)
    public ParametroLegalResponse registrarVigencia(ParametroLegalRequest request) {
        var vigente = parametros.findByCodigoAndVigenteHastaIsNull(request.codigo());
        vigente.ifPresent(actual -> {
            if (!request.vigenteDesde().isAfter(actual.getVigenteDesde())) {
                throw new ReglaDeNegocioException(
                        "La nueva vigencia debe comenzar después del " + actual.getVigenteDesde() + ".");
            }
            actual.cerrarVigenciaEl(request.vigenteDesde().minusDays(1));
        });
        ParametroLegal nuevo = parametros.saveAndFlush(new ParametroLegal(
                request.codigo(), request.descripcion(), request.valor(), request.vigenteDesde()));
        ParametroLegalResponse respuesta = mapper.aResponse(nuevo);
        Object antes = vigente.map(mapper::aResponse).orElse(null);
        eventos.publishEvent(new CambioRegistrado("PARAMETRO_LEGAL", nuevo.getId(), null, Accion.CREAR, antes, respuesta));
        return respuesta;
    }
}
