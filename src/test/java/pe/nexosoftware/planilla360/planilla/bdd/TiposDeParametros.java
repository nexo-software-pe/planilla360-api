package pe.nexosoftware.planilla360.planilla.bdd;

import io.cucumber.java.ParameterType;
import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoComision;

/**
 * Tipos de parámetro propios. Los montos usan punto decimal fijo: así no dependen del idioma de la característica
 * (en español, Cucumber interpretaría "3000.00" con la configuración regional de es).
 */
public class TiposDeParametros {

    @ParameterType("-?\\d+(?:\\.\\d+)?")
    public Dinero soles(String valor) {
        return Dinero.de(valor);
    }

    @ParameterType("\\d+(?:\\.\\d+)?")
    public BigDecimal porcentaje(String valor) {
        return new BigDecimal(valor);
    }

    @ParameterType("flujo|mixta")
    public TipoComision tipoComision(String valor) {
        return TipoComision.valueOf(valor.toUpperCase(java.util.Locale.ROOT));
    }

    @ParameterType("general|de pequeña empresa")
    public RegimenLaboral regimenLaboral(String valor) {
        return "general".equals(valor) ? RegimenLaboral.GENERAL : RegimenLaboral.PEQUENA_EMPRESA;
    }
}
