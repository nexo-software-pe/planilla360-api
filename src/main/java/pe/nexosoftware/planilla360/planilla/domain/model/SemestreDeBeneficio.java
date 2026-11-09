package pe.nexosoftware.planilla360.planilla.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

/** Semestre que se computa para una gratificación (ene-jun, jul-dic) o una CTS (nov-abr, may-oct). */
public record SemestreDeBeneficio(LocalDate inicio, LocalDate fin) {

    public SemestreDeBeneficio {
        Objects.requireNonNull(inicio, "El inicio del semestre es obligatorio");
        Objects.requireNonNull(fin, "El fin del semestre es obligatorio");
    }

    /** Semestre de la gratificación que se paga en el periodo (julio o diciembre). */
    public static SemestreDeBeneficio deGratificacion(Periodo periodo) {
        int anio = periodo.anio();
        return switch (periodo.mes()) {
            case 7 -> new SemestreDeBeneficio(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 6, 30));
            case 12 -> new SemestreDeBeneficio(LocalDate.of(anio, 7, 1), LocalDate.of(anio, 12, 31));
            default -> throw new ReglaDePlanillaException(
                    "La gratificación solo se paga en julio o diciembre, no en el periodo " + periodo);
        };
    }

    /** Semestre de la CTS que se deposita en el periodo (mayo o noviembre). */
    public static SemestreDeBeneficio deCts(Periodo periodo) {
        int anio = periodo.anio();
        return switch (periodo.mes()) {
            case 5 -> new SemestreDeBeneficio(LocalDate.of(anio - 1, 11, 1), LocalDate.of(anio, 4, 30));
            case 11 -> new SemestreDeBeneficio(LocalDate.of(anio, 5, 1), LocalDate.of(anio, 10, 31));
            default -> throw new ReglaDePlanillaException(
                    "La CTS solo se deposita en mayo o noviembre, no en el periodo " + periodo);
        };
    }
}
