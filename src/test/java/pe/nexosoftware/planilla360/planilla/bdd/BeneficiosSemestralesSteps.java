package pe.nexosoftware.planilla360.planilla.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.service.CalculadoraCts;
import pe.nexosoftware.planilla360.planilla.domain.service.CalculadoraGratificacion;

/** Pasos de gratificación y CTS (beneficios que se pagan por semestre). */
public class BeneficiosSemestralesSteps {

    private final ContextoDePlanilla contexto;
    private final CalculadoraGratificacion calculadoraGratificacion = new CalculadoraGratificacion();
    private final CalculadoraCts calculadoraCts = new CalculadoraCts(calculadoraGratificacion);

    public BeneficiosSemestralesSteps(ContextoDePlanilla contexto) {
        this.contexto = contexto;
    }

    @Dado("un trabajador del régimen {regimenLaboral} con sueldo básico de {soles} soles y asignación familiar de "
            + "{soles} soles")
    public void unTrabajadorDelRegimen(RegimenLaboral regimen, Dinero basico, Dinero asignacion) {
        contexto.regimenLaboral = regimen;
        contexto.basico = basico;
        contexto.asignacionFamiliar = asignacion;
    }

    @Dado("que trabajó {int} meses completos en el semestre")
    public void queTrabajoMesesCompletos(int meses) {
        contexto.mesesCompletos = meses;
    }

    @Dado("que trabajó {int} meses y {int} días completos en el semestre")
    public void queTrabajoMesesYDiasCompletos(int meses, int dias) {
        contexto.mesesCompletos = meses;
        contexto.diasCompletos = dias;
    }

    @Dado("que hizo horas extra por {soles} soles en cada uno de {int} meses del semestre")
    public void queHizoHorasExtra(Dinero monto, int meses) {
        for (int i = 0; i < meses; i++) {
            contexto.horasExtra.add(monto);
        }
    }

    @Dado("que su última gratificación fue de {soles} soles")
    public void queSuUltimaGratificacionFue(Dinero monto) {
        contexto.ultimaGratificacion = monto;
    }

    @Cuando("se calcula su gratificación")
    public void seCalculaSuGratificacion() {
        contexto.gratificacion = calculadoraGratificacion.calcular(contexto.basico, contexto.asignacionFamiliar,
                contexto.horasExtra, contexto.mesesCompletos, contexto.regimenLaboral, contexto.parametros);
    }

    @Cuando("se calcula su CTS")
    public void seCalculaSuCts() {
        contexto.depositoCts = calculadoraCts.calcular(contexto.basico, contexto.asignacionFamiliar,
                contexto.horasExtra, contexto.ultimaGratificacion, contexto.mesesCompletos, contexto.diasCompletos,
                contexto.regimenLaboral);
    }

    @Entonces("la gratificación es de {soles} soles")
    public void laGratificacionEs(Dinero esperado) {
        assertThat(contexto.gratificacion.monto()).isEqualTo(esperado);
    }

    @Entonces("la bonificación extraordinaria es de {soles} soles")
    public void laBonificacionExtraordinariaEs(Dinero esperado) {
        assertThat(contexto.gratificacion.bonificacionExtraordinaria()).isEqualTo(esperado);
    }

    @Entonces("recibe en total {soles} soles")
    public void recibeEnTotal(Dinero esperado) {
        assertThat(contexto.gratificacion.total()).isEqualTo(esperado);
    }

    @Entonces("su remuneración computable es de {soles} soles")
    public void suRemuneracionComputableEs(Dinero esperado) {
        assertThat(contexto.depositoCts.remuneracionComputable()).isEqualTo(esperado);
    }

    @Entonces("el depósito de CTS es de {soles} soles")
    public void elDepositoDeCtsEs(Dinero esperado) {
        assertThat(contexto.depositoCts.monto()).isEqualTo(esperado);
    }
}
