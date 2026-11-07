package pe.nexosoftware.planilla360.planilla.domain.regimen;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Descuento pensionario del mes desglosado (en ONP solo hay aporte). Esqueleto. */
public record DescuentoPension(Dinero aporte, Dinero prima, Dinero comision) {

    public Dinero total() {
        throw new UnsupportedOperationException("pendiente");
    }
}
