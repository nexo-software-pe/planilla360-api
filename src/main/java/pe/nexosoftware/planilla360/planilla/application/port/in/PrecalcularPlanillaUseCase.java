package pe.nexosoftware.planilla360.planilla.application.port.in;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PrecalcularPlanillaUseCase {

    /**
     * Si {@code hoy} es el último día hábil del mes y la empresa aún no tiene planilla mensual vigente de ese mes, la
     * crea y la calcula en borrador.
     *
     * @return la planilla pre-calculada, o vacío si no correspondía hacer nada
     */
    Optional<UUID> precalcularSiCorresponde(LocalDate hoy);

    /** Aviso del día para RRHH: gratificación en julio y diciembre, CTS en mayo y noviembre. */
    Optional<String> recordatorioDeBeneficio(LocalDate hoy);
}
