package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Eslabón de la cadena de validaciones de cierre (Chain of Responsibility). A diferencia de la versión clásica, ningún
 * eslabón corta la cadena: se reúnen todas las incidencias para mostrarlas juntas al usuario.
 */
public abstract class EslabonValidacion {

    private EslabonValidacion siguiente;

    /** Encadena el siguiente eslabón y lo devuelve, para escribir {@code a.luego(b).luego(c)}. */
    public final EslabonValidacion luego(EslabonValidacion proximo) {
        this.siguiente = Objects.requireNonNull(proximo, "El siguiente eslabón es obligatorio");
        return proximo;
    }

    public final List<Incidencia> validar(ContextoCierre contexto) {
        List<Incidencia> incidencias = new ArrayList<>(revisar(contexto));
        if (siguiente != null) {
            incidencias.addAll(siguiente.validar(contexto));
        }
        return List.copyOf(incidencias);
    }

    /** Incidencias que detecta este eslabón (sin consultar a los demás). */
    protected abstract List<Incidencia> revisar(ContextoCierre contexto);
}
