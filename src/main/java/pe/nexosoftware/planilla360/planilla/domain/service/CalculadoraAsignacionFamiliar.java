package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/**
 * Asignación familiar: un monto fijo por trabajador (porcentaje de la RMV) si tiene al menos un hijo elegible,
 * es decir menor de 18 años o, si estudia, hasta los 24 años.
 */
public final class CalculadoraAsignacionFamiliar {

    private static final int EDAD_MAYORIA = 18;
    private static final int EDAD_LIMITE_ESTUDIANTE = 25;

    public int hijosElegibles(List<Hijo> hijos, LocalDate al) {
        int elegibles = 0;
        for (Hijo hijo : hijos) {
            if (esElegible(hijo, al)) {
                elegibles++;
            }
        }
        return elegibles;
    }

    public Dinero calcular(List<Hijo> hijos, LocalDate al, ParametrosLegales parametros) {
        if (hijosElegibles(hijos, al) == 0) {
            return Dinero.CERO;
        }
        return parametros.rmv().porcentaje(parametros.pctAsignacionFamiliar());
    }

    private static boolean esElegible(Hijo hijo, LocalDate al) {
        if (hijo.nacimiento().isAfter(al)) {
            throw new IllegalArgumentException("El hijo no puede nacer después de la fecha de cálculo: " + hijo);
        }
        int edad = Period.between(hijo.nacimiento(), al).getYears();
        return edad < EDAD_MAYORIA || (hijo.estudia() && edad < EDAD_LIMITE_ESTUDIANTE);
    }
}
