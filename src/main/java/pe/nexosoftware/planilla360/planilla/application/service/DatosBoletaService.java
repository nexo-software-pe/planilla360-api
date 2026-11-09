package pe.nexosoftware.planilla360.planilla.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.planilla.PlanillaApi;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Afp;

/** Responde al módulo de boletas con los datos de cada línea de la planilla (sin exponer el dominio). */
@Service
@Transactional(readOnly = true)
public class DatosBoletaService implements PlanillaApi {

    private final PlanillaPort planillas;

    public DatosBoletaService(PlanillaPort planillas) {
        this.planillas = planillas;
    }

    @Override
    public Optional<DatosParaBoletas> datosParaBoletas(UUID planillaId) {
        return planillas.buscar(planillaId).map(DatosBoletaService::aDatos);
    }

    private static DatosParaBoletas aDatos(Planilla planilla) {
        List<LineaBoleta> lineas = planilla.lineas().stream().map(DatosBoletaService::aLinea).toList();
        return new DatosParaBoletas(
                planilla.id(),
                planilla.periodo().valor(),
                planilla.tipo().name(),
                planilla.estado().name(),
                lineas);
    }

    private static LineaBoleta aLinea(LineaPlanilla linea) {
        List<ConceptoBoleta> conceptos = linea.conceptos().stream()
                .map(c -> new ConceptoBoleta(c.codigo(), c.descripcion(), c.tipo().name(), c.monto().monto()))
                .toList();
        return new LineaBoleta(
                linea.empleadoId(),
                linea.regimenPensionario() instanceof Afp afp
                        ? "AFP " + afp.nombre()
                        : "ONP",
                conceptos,
                linea.totalIngresos().monto(),
                linea.totalDescuentos().monto(),
                linea.neto().monto());
    }
}
