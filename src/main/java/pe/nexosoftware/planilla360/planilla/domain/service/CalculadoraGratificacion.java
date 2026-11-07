package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.Month;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.Gratificacion;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

/**
 * Gratificación de julio (ene-jun) y diciembre (jul-dic): una remuneración computable por semestre, proporcional a
 * los meses completos, más la bonificación extraordinaria. Las horas extra solo cuentan si hubo en 3 meses o más.
 */
public final class CalculadoraGratificacion {

    private static final int MESES_DEL_SEMESTRE = 6;
    private static final int MESES_MINIMOS_CON_HORAS_EXTRA = 3;

    public boolean esMesDeGratificacion(Periodo periodo) {
        return periodo.mes() == Month.JULY.getValue() || periodo.mes() == Month.DECEMBER.getValue();
    }

    /** Básico + asignación familiar + promedio semestral de horas extra (si hubo en 3 meses o más). */
    public Dinero remuneracionComputable(Dinero basico, Dinero asignacionFamiliar, List<Dinero> horasExtraDelSemestre) {
        return basico.sumar(asignacionFamiliar).sumar(promedioHorasExtra(horasExtraDelSemestre));
    }

    public Gratificacion calcular(Dinero basico, Dinero asignacionFamiliar, List<Dinero> horasExtraDelSemestre,
            int mesesCompletos, RegimenLaboral regimen, ParametrosLegales parametros) {
        if (mesesCompletos < 0 || mesesCompletos > MESES_DEL_SEMESTRE) {
            throw new IllegalArgumentException("Los meses completos deben estar entre 0 y 6: " + mesesCompletos);
        }
        Dinero computable = remuneracionComputable(basico, asignacionFamiliar, horasExtraDelSemestre);
        Dinero proporcional = computable.proporcional(mesesCompletos, MESES_DEL_SEMESTRE);
        Dinero monto = regimen.aplicarBeneficio(proporcional);
        Dinero bonificacion = monto.porcentaje(parametros.pctBonificacionExtraordinaria());
        return new Gratificacion(monto, bonificacion);
    }

    private static Dinero promedioHorasExtra(List<Dinero> horasExtra) {
        if (horasExtra.size() > MESES_DEL_SEMESTRE) {
            throw new IllegalArgumentException("Solo se admiten los 6 meses del semestre: " + horasExtra.size());
        }
        int mesesConHorasExtra = 0;
        Dinero total = Dinero.CERO;
        for (Dinero mes : horasExtra) {
            if (mes.esNegativo()) {
                throw new IllegalArgumentException("Las horas extra no pueden ser negativas");
            }
            if (!mes.esCero()) {
                mesesConHorasExtra++;
            }
            total = total.sumar(mes);
        }
        if (mesesConHorasExtra < MESES_MINIMOS_CON_HORAS_EXTRA) {
            return Dinero.CERO;
        }
        return total.proporcional(1, MESES_DEL_SEMESTRE);
    }
}
