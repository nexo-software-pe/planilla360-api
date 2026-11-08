package pe.nexosoftware.planilla360.planilla.infrastructure.web.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.model.PagoPlanilla;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Concepto;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.Incidencia;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.ConceptoResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.LineaPlanillaResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.PagoPlanillaResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.PlanillaResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.SimulacionResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.ValidacionResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.ValidacionResponse.IncidenciaResponse;

/** Convierte el modelo de planilla (con {@link Dinero} y {@link Periodo}) a los DTO de la API. */
@Mapper(componentModel = "spring")
public interface PlanillaWebMapper {

    PlanillaResponse aResponse(ResumenPlanilla resumen);

    default PlanillaResponse aResponse(Planilla planilla) {
        return aResponse(ResumenPlanilla.de(planilla));
    }

    PagoPlanillaResponse aResponse(PagoPlanilla pago);

    ConceptoResponse aResponse(Concepto concepto);

    IncidenciaResponse aResponse(Incidencia incidencia);

    default ValidacionResponse aResponse(ResultadoValidacion resultado) {
        List<IncidenciaResponse> incidencias = resultado.incidencias().stream().map(this::aResponse).toList();
        return new ValidacionResponse(resultado.esValida(), incidencias);
    }

    default LineaPlanillaResponse aResponse(LineaPlanilla l) {
        return new LineaPlanillaResponse(
                l.empleadoId(),
                monto(l.basico()),
                monto(l.asignacionFamiliar()),
                monto(l.horasExtra()),
                monto(l.bonos()),
                monto(l.gratificacion()),
                monto(l.bonificacionExtraordinaria()),
                monto(l.cts()),
                l.regimenPensionario().nombre(),
                monto(l.descuentoPension().total()),
                monto(l.retencion5ta()),
                monto(l.descuentoAsistencia()),
                monto(l.otrosDescuentos()),
                monto(l.aporteEssalud()),
                monto(l.totalIngresos()),
                monto(l.totalDescuentos()),
                monto(l.neto()),
                l.conceptos().stream().map(this::aResponse).toList());
    }

    default SimulacionResponse aSimulacion(Planilla planilla) {
        return new SimulacionResponse(
                planilla.periodo().toString(),
                planilla.tipo(),
                planilla.lineas().size(),
                monto(planilla.totalIngresos()),
                monto(planilla.totalDescuentos()),
                monto(planilla.totalNeto()),
                monto(planilla.totalEssalud()),
                planilla.lineas().stream().map(this::aResponse).toList());
    }

    default BigDecimal monto(Dinero dinero) {
        return dinero.monto();
    }

    default String periodo(Periodo periodo) {
        return periodo.toString();
    }
}
