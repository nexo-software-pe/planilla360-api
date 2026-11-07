package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.Month;
import java.util.List;
import java.util.Objects;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DepositoCts;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

/**
 * CTS de mayo (nov-abr) y noviembre (may-oct). Remuneración computable = básico + asignación familiar + promedio de
 * horas extra + 1/6 de la última gratificación; CTS = computable/12 por mes completo + computable/12/30 por día.
 */
public final class CalculadoraCts {

    private static final int MESES_DEL_SEMESTRE = 6;
    private static final int DIAS_POR_MES = 30;
    private static final int DIAS_POR_ANIO_COMERCIAL = 12 * DIAS_POR_MES;

    private final CalculadoraGratificacion gratificacion;

    public CalculadoraCts(CalculadoraGratificacion gratificacion) {
        this.gratificacion = Objects.requireNonNull(gratificacion);
    }

    public boolean esMesDeDeposito(Periodo periodo) {
        return periodo.mes() == Month.MAY.getValue() || periodo.mes() == Month.NOVEMBER.getValue();
    }

    public DepositoCts calcular(Dinero basico, Dinero asignacionFamiliar, List<Dinero> horasExtraDelSemestre,
            Dinero ultimaGratificacion, int mesesCompletos, int diasCompletos, RegimenLaboral regimen) {
        exigirTiempoValido(mesesCompletos, diasCompletos);
        if (ultimaGratificacion.esNegativo()) {
            throw new IllegalArgumentException("La última gratificación no puede ser negativa");
        }
        Dinero computable = gratificacion.remuneracionComputable(basico, asignacionFamiliar, horasExtraDelSemestre)
                .sumar(ultimaGratificacion.proporcional(1, MESES_DEL_SEMESTRE));
        long diasEquivalentes = (long) mesesCompletos * DIAS_POR_MES + diasCompletos;
        Dinero cts = computable.proporcional(diasEquivalentes, DIAS_POR_ANIO_COMERCIAL);
        return new DepositoCts(computable, regimen.aplicarBeneficio(cts));
    }

    private static void exigirTiempoValido(int meses, int dias) {
        if (meses < 0 || meses > MESES_DEL_SEMESTRE) {
            throw new IllegalArgumentException("Los meses completos deben estar entre 0 y 6: " + meses);
        }
        if (dias < 0 || dias >= DIAS_POR_MES) {
            throw new IllegalArgumentException("Los días completos deben estar entre 0 y 29: " + dias);
        }
        if (meses == MESES_DEL_SEMESTRE && dias > 0) {
            throw new IllegalArgumentException("Un semestre no puede superar los 6 meses");
        }
    }
}
