package pe.nexosoftware.planilla360.planilla.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

/**
 * Datos de un trabajador para calcular su gratificación o su CTS.
 *
 * @param horasExtraDelSemestre horas extra pagadas en cada mes del semestre (solo los meses con planilla cerrada)
 * @param tiempo meses y días completos que computa en el semestre
 * @param ultimaGratificacion última gratificación pagada (sin bonificación); solo la usa la CTS
 */
public record DatosBeneficioSemestral(
        UUID empleadoId,
        Dinero sueldoBasico,
        RegimenPensionario regimenPensionario,
        RegimenLaboral regimenLaboral,
        List<Hijo> hijos,
        List<Dinero> horasExtraDelSemestre,
        TiempoComputable tiempo,
        Dinero ultimaGratificacion) {

    public DatosBeneficioSemestral {
        Objects.requireNonNull(empleadoId, "El trabajador es obligatorio");
        Objects.requireNonNull(sueldoBasico, "El sueldo básico es obligatorio");
        Objects.requireNonNull(regimenPensionario, "El régimen pensionario es obligatorio");
        Objects.requireNonNull(regimenLaboral, "El régimen laboral es obligatorio");
        Objects.requireNonNull(tiempo, "El tiempo computable es obligatorio");
        Objects.requireNonNull(ultimaGratificacion, "La última gratificación es obligatoria");
        hijos = List.copyOf(hijos);
        horasExtraDelSemestre = List.copyOf(horasExtraDelSemestre);
    }
}
