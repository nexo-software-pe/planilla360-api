package pe.nexosoftware.planilla360.planilla.bdd;

import java.util.ArrayList;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.DepositoCts;
import pe.nexosoftware.planilla360.planilla.domain.model.Gratificacion;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;
import pe.nexosoftware.planilla360.planilla.domain.regimen.DescuentoPension;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

/** Estado compartido entre los pasos de un escenario (PicoContainer crea una instancia por escenario). */
public class ContextoDePlanilla {

    ParametrosLegales parametros;

    Dinero remuneracionAsegurable;
    RegimenPensionario regimenPensionario;
    DescuentoPension descuentoPension;

    RegimenLaboral regimenLaboral;
    Dinero basico = Dinero.CERO;
    Dinero asignacionFamiliar = Dinero.CERO;
    final List<Dinero> horasExtra = new ArrayList<>();
    Dinero ultimaGratificacion = Dinero.CERO;
    int mesesCompletos;
    int diasCompletos;

    Gratificacion gratificacion;
    DepositoCts depositoCts;
}
