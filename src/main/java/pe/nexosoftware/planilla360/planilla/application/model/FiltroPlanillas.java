package pe.nexosoftware.planilla360.planilla.application.model;

import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

/** Criterios opcionales para listar planillas ({@code null} = sin filtrar por ese criterio). */
public record FiltroPlanillas(Periodo periodo, TipoPlanilla tipo, EstadoPlanilla estado) {
}
