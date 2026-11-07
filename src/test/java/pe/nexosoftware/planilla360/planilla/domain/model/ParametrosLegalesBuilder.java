package pe.nexosoftware.planilla360.planilla.domain.model;

import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Test data builder: parte de los valores de referencia del seed (RMV 1 130, UIT 5 350, ONP 13 %...). */
public final class ParametrosLegalesBuilder {

    private Dinero rmv = Dinero.de("1130");
    private Dinero uit = Dinero.de("5350");
    private BigDecimal pctAsignacionFamiliar = new BigDecimal("10");
    private BigDecimal pctOnp = new BigDecimal("13");
    private BigDecimal pctAfpAporte = new BigDecimal("10");
    private BigDecimal pctAfpPrima = new BigDecimal("1.37");
    private BigDecimal pctEssalud = new BigDecimal("9");
    private BigDecimal pctBonificacionExtraordinaria = new BigDecimal("9");

    private ParametrosLegalesBuilder() {
    }

    public static ParametrosLegalesBuilder parametros() {
        return new ParametrosLegalesBuilder();
    }

    public static ParametrosLegales referencia() {
        return parametros().build();
    }

    public ParametrosLegalesBuilder conRmv(String valor) {
        this.rmv = Dinero.de(valor);
        return this;
    }

    public ParametrosLegalesBuilder conUit(String valor) {
        this.uit = Dinero.de(valor);
        return this;
    }

    public ParametrosLegalesBuilder conPctOnp(String valor) {
        this.pctOnp = new BigDecimal(valor);
        return this;
    }

    public ParametrosLegalesBuilder conPctAfpAporte(String valor) {
        this.pctAfpAporte = new BigDecimal(valor);
        return this;
    }

    public ParametrosLegalesBuilder conPctAfpPrima(String valor) {
        this.pctAfpPrima = new BigDecimal(valor);
        return this;
    }

    public ParametrosLegalesBuilder conPctAsignacionFamiliar(String valor) {
        this.pctAsignacionFamiliar = new BigDecimal(valor);
        return this;
    }

    public ParametrosLegalesBuilder conPctEssalud(String valor) {
        this.pctEssalud = new BigDecimal(valor);
        return this;
    }

    public ParametrosLegalesBuilder conPctBonificacionExtraordinaria(String valor) {
        this.pctBonificacionExtraordinaria = new BigDecimal(valor);
        return this;
    }

    public ParametrosLegales build() {
        return new ParametrosLegales(rmv, uit, pctAsignacionFamiliar, pctOnp, pctAfpAporte, pctAfpPrima,
                pctEssalud, pctBonificacionExtraordinaria);
    }
}
