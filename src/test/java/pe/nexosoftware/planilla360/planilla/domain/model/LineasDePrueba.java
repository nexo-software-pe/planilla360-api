package pe.nexosoftware.planilla360.planilla.domain.model;

import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;

/** Test data builders de líneas de planilla con identificadores deterministas. */
public final class LineasDePrueba {

    private LineasDePrueba() {
    }

    public static UUID empleado(int n) {
        return new UUID(0L, n);
    }

    /** Línea ONP con sueldo básico 3 000 (neto 2 610,00) del trabajador n. */
    public static LineaPlanilla linea(int n) {
        return lineaConBasico(n, "3000");
    }

    public static LineaPlanilla lineaConBasico(int n, String basico) {
        return LineaPlanilla.de(empleado(n)).basico(Dinero.de(basico)).regimenPensionario(new Onp())
                .build(referencia());
    }

    public static LineaPlanilla lineaConNetoNegativo(int n) {
        return LineaPlanilla.de(empleado(n)).basico(Dinero.de("1000")).otrosDescuentos(Dinero.de("2000"))
                .regimenPensionario(new Onp()).build(referencia());
    }
}
