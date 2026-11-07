package pe.nexosoftware.planilla360.planilla.domain.regimen;

import java.util.Objects;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

/** Factory: elige la estrategia pensionaria según los datos del contrato del trabajador. */
public final class FabricaRegimenPensionario {

    private FabricaRegimenPensionario() {
    }

    public static RegimenPensionario crear(TipoRegimenPensionario tipo, AfpVigente afp, TipoComision comision) {
        Objects.requireNonNull(tipo, "El tipo de régimen pensionario es obligatorio");
        return switch (tipo) {
            case ONP -> new Onp();
            case AFP -> crearAfp(afp, comision);
        };
    }

    private static Afp crearAfp(AfpVigente afp, TipoComision comision) {
        if (afp == null) {
            throw new ReglaDePlanillaException("El trabajador en AFP debe tener una AFP asignada");
        }
        if (comision == null) {
            throw new ReglaDePlanillaException("El trabajador en AFP debe tener definido el tipo de comisión");
        }
        return new Afp(afp.nombre(), afp.comision(comision));
    }
}
