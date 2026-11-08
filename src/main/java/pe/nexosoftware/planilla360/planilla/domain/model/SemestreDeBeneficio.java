package pe.nexosoftware.planilla360.planilla.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;

/** Semestre que se computa para una gratificación (ene-jun, jul-dic) o una CTS (nov-abr, may-oct). */
public record SemestreDeBeneficio(LocalDate inicio, LocalDate fin) {

    public SemestreDeBeneficio {
        Objects.requireNonNull(inicio, "El inicio del semestre es obligatorio");
        Objects.requireNonNull(fin, "El fin del semestre es obligatorio");
    }

    /** Semestre de la gratificación que se paga en el periodo (julio o diciembre). */
    public static SemestreDeBeneficio deGratificacion(Periodo periodo) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Semestre de la CTS que se deposita en el periodo (mayo o noviembre). */
    public static SemestreDeBeneficio deCts(Periodo periodo) {
        throw new UnsupportedOperationException("pendiente");
    }
}
