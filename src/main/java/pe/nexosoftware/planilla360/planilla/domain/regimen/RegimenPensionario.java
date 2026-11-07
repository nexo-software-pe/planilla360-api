package pe.nexosoftware.planilla360.planilla.domain.regimen;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/** Estrategia de descuento pensionario: ONP o AFP. */
public sealed interface RegimenPensionario permits Onp, Afp {

    String nombre();

    /** Descuento del mes sobre la remuneración asegurable (sin gratificación ni CTS). */
    DescuentoPension calcular(Dinero remuneracionAsegurable, ParametrosLegales parametros);
}
