package pe.nexosoftware.planilla360.integracion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tipo de cambio del día.
 *
 * @param desactualizado {@code true} cuando el servicio no respondió y se devuelve el último valor conocido
 */
public record TipoCambio(LocalDate fecha, String moneda, BigDecimal compra, BigDecimal venta, boolean desactualizado) {

    public TipoCambio comoDesactualizado() {
        return new TipoCambio(fecha, moneda, compra, venta, true);
    }
}
