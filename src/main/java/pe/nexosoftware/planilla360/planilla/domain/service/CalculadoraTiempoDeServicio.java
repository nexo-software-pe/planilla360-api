package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.LocalDate;
import pe.nexosoftware.planilla360.planilla.domain.model.SemestreDeBeneficio;
import pe.nexosoftware.planilla360.planilla.domain.model.TiempoComputable;

/** Meses y días completos que un trabajador computa en un semestre, según su ingreso y su cese. */
public final class CalculadoraTiempoDeServicio {

    /**
     * @param ingreso fecha de ingreso del trabajador
     * @param cese fecha de cese, o {@code null} si sigue vigente
     */
    public TiempoComputable computar(LocalDate ingreso, LocalDate cese, SemestreDeBeneficio semestre) {
        throw new UnsupportedOperationException("pendiente");
    }
}
