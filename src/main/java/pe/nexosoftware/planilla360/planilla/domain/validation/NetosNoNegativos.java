package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Ningún trabajador puede quedar con neto a pagar negativo. */
public final class NetosNoNegativos extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        return contexto.planilla().lineas().stream()
                .filter(linea -> linea.neto().esNegativo())
                .map(linea -> new Incidencia("NETO_NEGATIVO",
                        "El trabajador " + linea.empleadoId() + " tiene un neto negativo: " + linea.neto()))
                .toList();
    }
}
