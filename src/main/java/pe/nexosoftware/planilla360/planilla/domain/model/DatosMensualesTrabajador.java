package pe.nexosoftware.planilla360.planilla.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

/**
 * Lo que la capa de aplicación reúne de otros módulos para calcular el mes de un trabajador: contrato, familiares,
 * asistencia del mes, horas extra aprobadas y lo ya percibido en el año.
 *
 * @param horasJornadaDiaria horas de la jornada diaria (ver {@code JornadaLaboral}); define el valor de la hora
 * @param diasLaborados días calendario del mes en que el trabajador estuvo vigente (mes completo = días del mes)
 * @param diasInasistencia faltas injustificadas más días de permiso sin goce
 * @param horasExtraPorDia horas extra aprobadas, una entrada por día
 * @param ingresosYaPercibidos ingresos del año anteriores al periodo, para la proyección de renta de 5.ª
 */
public record DatosMensualesTrabajador(
        UUID empleadoId,
        Dinero sueldoBasico,
        int horasJornadaDiaria,
        RegimenPensionario regimenPensionario,
        RegimenLaboral regimenLaboral,
        List<Hijo> hijos,
        int diasLaborados,
        int minutosTardanza,
        int diasInasistencia,
        List<BigDecimal> horasExtraPorDia,
        Dinero ingresosYaPercibidos) {

    public DatosMensualesTrabajador {
        Objects.requireNonNull(empleadoId, "El trabajador es obligatorio");
        Objects.requireNonNull(sueldoBasico, "El sueldo básico es obligatorio");
        Objects.requireNonNull(regimenPensionario, "El régimen pensionario es obligatorio");
        Objects.requireNonNull(regimenLaboral, "El régimen laboral es obligatorio");
        Objects.requireNonNull(ingresosYaPercibidos, "Los ingresos ya percibidos son obligatorios");
        hijos = List.copyOf(hijos);
        horasExtraPorDia = List.copyOf(horasExtraPorDia);
    }
}
