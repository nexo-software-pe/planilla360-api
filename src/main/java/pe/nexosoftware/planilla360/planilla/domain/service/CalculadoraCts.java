package pe.nexosoftware.planilla360.planilla.domain.service;

import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DepositoCts;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;

/** CTS de mayo y noviembre: esqueleto, se construye test-first. */
public final class CalculadoraCts {

    private final CalculadoraGratificacion gratificacion;

    public CalculadoraCts(CalculadoraGratificacion gratificacion) {
        this.gratificacion = gratificacion;
    }

    public boolean esMesDeDeposito(Periodo periodo) {
        throw new UnsupportedOperationException("pendiente");
    }

    public DepositoCts calcular(Dinero basico, Dinero asignacionFamiliar, List<Dinero> horasExtraDelSemestre,
            Dinero ultimaGratificacion, int mesesCompletos, int diasCompletos, RegimenLaboral regimen) {
        throw new UnsupportedOperationException("pendiente");
    }
}
