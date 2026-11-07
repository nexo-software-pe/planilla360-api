package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;

/**
 * Datos que el dominio no posee y que la capa de aplicación consulta a otros módulos (por puertos) antes de cerrar:
 * trabajadores sin régimen pensionario, estado de la asistencia, vacaciones por aprobar y planillas ya cerradas.
 */
public record ContextoCierre(
        Planilla planilla,
        Set<UUID> empleadosSinRegimenPensionario,
        boolean asistenciaCompleta,
        int vacacionesPendientesQueSolapan,
        boolean existeOtraPlanillaCerradaDelPeriodo) {

    public ContextoCierre {
        Objects.requireNonNull(planilla, "La planilla es obligatoria");
        empleadosSinRegimenPensionario = Set.copyOf(empleadosSinRegimenPensionario);
        if (vacacionesPendientesQueSolapan < 0) {
            throw new IllegalArgumentException("Las vacaciones pendientes no pueden ser negativas");
        }
    }
}
