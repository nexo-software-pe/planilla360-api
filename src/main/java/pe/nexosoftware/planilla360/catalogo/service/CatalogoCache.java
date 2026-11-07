package pe.nexosoftware.planilla360.catalogo.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.catalogo.dto.AfpResponse;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalResponse;
import pe.nexosoftware.planilla360.catalogo.mapper.CatalogoMapper;
import pe.nexosoftware.planilla360.catalogo.repository.AfpRepository;
import pe.nexosoftware.planilla360.catalogo.repository.ParametroLegalRepository;

/**
 * Lecturas cacheadas de catálogos globales (cambian muy poco). Vive en un bean aparte para que el
 * proxy de caché se aplique también cuando otros servicios del módulo lo invocan.
 */
@Component
public class CatalogoCache {

    public static final String PARAMETROS = "parametros-legales";
    public static final String AFPS = "afps";

    private final ParametroLegalRepository parametros;
    private final AfpRepository afps;
    private final CatalogoMapper mapper;

    public CatalogoCache(ParametroLegalRepository parametros, AfpRepository afps, CatalogoMapper mapper) {
        this.parametros = parametros;
        this.afps = afps;
        this.mapper = mapper;
    }

    @Cacheable(PARAMETROS)
    public List<ParametroLegalResponse> parametros() {
        return new ArrayList<>(mapper.aParametroResponses(parametros.findAllByOrderByCodigoAscVigenteDesdeDesc()));
    }

    @Cacheable(AFPS)
    public List<AfpResponse> afps() {
        return new ArrayList<>(mapper.aAfpResponses(afps.findAllByOrderByNombreAsc()));
    }
}
