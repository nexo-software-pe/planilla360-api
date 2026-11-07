package pe.nexosoftware.planilla360.planilla.domain.regimen;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Regla común de las estrategias: la base de cálculo nunca es negativa. */
final class BaseAsegurable {

    private BaseAsegurable() {
    }

    static Dinero exigirValida(Dinero remuneracionAsegurable) {
        if (remuneracionAsegurable.esNegativo()) {
            throw new IllegalArgumentException("La remuneración asegurable no puede ser negativa");
        }
        return remuneracionAsegurable;
    }
}
