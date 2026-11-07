package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.Set;
import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;

/**
 * Datos que el dominio no posee y que la capa de aplicación consulta a otros módulos (por puertos) antes de cerrar.
 * Esqueleto.
 */
public record ContextoCierre(
        Planilla planilla,
        Set<UUID> empleadosSinRegimenPensionario,
        boolean asistenciaCompleta,
        int vacacionesPendientesQueSolapan,
        boolean existeOtraPlanillaCerradaDelPeriodo) {
}
