package pe.nexosoftware.planilla360.planilla.domain.regimen;

import java.math.BigDecimal;
import java.util.Objects;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/**
 * Sistema privado de pensiones: aporte al fondo + prima de seguro + comisión de la administradora.
 * Cada componente se redondea por separado porque la boleta los muestra por separado.
 */
public record Afp(String administradora, BigDecimal comisionPct) implements RegimenPensionario {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public Afp {
        Objects.requireNonNull(administradora, "La administradora es obligatoria");
        Objects.requireNonNull(comisionPct, "La comisión es obligatoria");
        if (administradora.isBlank()) {
            throw new IllegalArgumentException("La administradora no puede estar vacía");
        }
        if (comisionPct.signum() < 0 || comisionPct.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("La comisión debe estar entre 0 y 100: " + comisionPct);
        }
    }

    @Override
    public String nombre() {
        return "AFP " + administradora;
    }

    @Override
    public DescuentoPension calcular(Dinero remuneracionAsegurable, ParametrosLegales parametros) {
        if (remuneracionAsegurable.esNegativo()) {
            throw new IllegalArgumentException("La remuneración asegurable no puede ser negativa");
        }
        return new DescuentoPension(
                remuneracionAsegurable.porcentaje(parametros.pctAfpAporte()),
                remuneracionAsegurable.porcentaje(parametros.pctAfpPrima()),
                remuneracionAsegurable.porcentaje(comisionPct));
    }
}
