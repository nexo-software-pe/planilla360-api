package pe.nexosoftware.planilla360.planilla.domain.model;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Gratificación semestral con su bonificación extraordinaria (no sufre descuento de AFP/ONP). */
public record Gratificacion(Dinero monto, Dinero bonificacionExtraordinaria) {

    public Dinero total() {
        return monto.sumar(bonificacionExtraordinaria);
    }
}
