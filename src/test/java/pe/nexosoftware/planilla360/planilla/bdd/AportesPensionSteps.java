package pe.nexosoftware.planilla360.planilla.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder;
import pe.nexosoftware.planilla360.planilla.domain.regimen.AfpVigente;
import pe.nexosoftware.planilla360.planilla.domain.regimen.FabricaRegimenPensionario;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoComision;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoRegimenPensionario;

/** Pasos de aportes ONP/AFP; usan la fábrica de regímenes, igual que lo hará la aplicación. */
public class AportesPensionSteps {

    private final ContextoDePlanilla contexto;

    public AportesPensionSteps(ContextoDePlanilla contexto) {
        this.contexto = contexto;
    }

    @Dado("los parámetros legales de referencia")
    public void losParametrosLegalesDeReferencia() {
        contexto.parametros = ParametrosLegalesBuilder.referencia();
    }

    @Dado("un trabajador con remuneración asegurable de {soles} soles")
    public void unTrabajadorConRemuneracionAsegurable(Dinero remuneracion) {
        contexto.remuneracionAsegurable = remuneracion;
    }

    @Dado("que está afiliado a la ONP")
    public void queEstaAfiliadoALaOnp() {
        contexto.regimenPensionario = FabricaRegimenPensionario.crear(TipoRegimenPensionario.ONP, null, null);
    }

    @Dado("que está afiliado a la AFP {word} con comisión {tipoComision} de {porcentaje} %")
    public void queEstaAfiliadoALaAfp(String nombre, TipoComision tipo, BigDecimal porcentaje) {
        BigDecimal flujo = tipo == TipoComision.FLUJO ? porcentaje : BigDecimal.ZERO;
        BigDecimal mixta = tipo == TipoComision.MIXTA ? porcentaje : BigDecimal.ZERO;
        contexto.regimenPensionario = FabricaRegimenPensionario.crear(TipoRegimenPensionario.AFP,
                new AfpVigente(nombre, flujo, mixta), tipo);
    }

    @Cuando("se calcula su descuento pensionario")
    public void seCalculaSuDescuentoPensionario() {
        contexto.descuentoPension = contexto.regimenPensionario.calcular(contexto.remuneracionAsegurable,
                contexto.parametros);
    }

    @Entonces("el aporte al fondo es de {soles} soles")
    public void elAporteAlFondoEs(Dinero esperado) {
        assertThat(contexto.descuentoPension.aporte()).isEqualTo(esperado);
    }

    @Entonces("la prima de seguro es de {soles} soles")
    public void laPrimaDeSeguroEs(Dinero esperado) {
        assertThat(contexto.descuentoPension.prima()).isEqualTo(esperado);
    }

    @Entonces("la comisión es de {soles} soles")
    public void laComisionEs(Dinero esperado) {
        assertThat(contexto.descuentoPension.comision()).isEqualTo(esperado);
    }

    @Entonces("el descuento total es de {soles} soles")
    public void elDescuentoTotalEs(Dinero esperado) {
        assertThat(contexto.descuentoPension.total()).isEqualTo(esperado);
    }
}
