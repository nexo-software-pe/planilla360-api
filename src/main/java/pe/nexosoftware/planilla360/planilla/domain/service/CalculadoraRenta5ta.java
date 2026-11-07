package pe.nexosoftware.planilla360.planilla.domain.service;

import java.math.BigDecimal;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/**
 * Retención de renta de 5.ª categoría (versión simplificada del MVP): se proyecta la renta anual, se deducen 7 UIT, se
 * aplica la escala acumulativa y la retención mensual es el impuesto anual entre 12.
 */
public final class CalculadoraRenta5ta {

    private static final int UIT_DEDUCIBLES = 7;
    private static final int MESES_DEL_ANIO = 12;

    /** Tramo de la escala: hasta cuántas UIT de renta neta aplica y a qué tasa (null = sin límite). */
    private record Tramo(Integer hastaUit, BigDecimal tasaPct) {
    }

    private static final List<Tramo> ESCALA = List.of(
            new Tramo(5, BigDecimal.valueOf(8)),
            new Tramo(20, BigDecimal.valueOf(14)),
            new Tramo(35, BigDecimal.valueOf(17)),
            new Tramo(45, BigDecimal.valueOf(20)),
            new Tramo(null, BigDecimal.valueOf(30)));

    public Dinero impuestoAnual(Dinero rentaBrutaAnual, ParametrosLegales parametros) {
        exigirNoNegativo(rentaBrutaAnual, "La renta bruta anual");
        Dinero uit = parametros.uit();
        Dinero rentaNeta = rentaBrutaAnual.restar(uit.multiplicar(BigDecimal.valueOf(UIT_DEDUCIBLES)));
        if (rentaNeta.esNegativo() || rentaNeta.esCero()) {
            return Dinero.CERO;
        }
        Dinero impuesto = Dinero.CERO;
        Dinero limiteAnterior = Dinero.CERO;
        for (Tramo tramo : ESCALA) {
            Dinero limite = tramo.hastaUit() == null ? rentaNeta
                    : uit.multiplicar(BigDecimal.valueOf(tramo.hastaUit())).min(rentaNeta);
            impuesto = impuesto.sumar(limite.restar(limiteAnterior).porcentaje(tramo.tasaPct()));
            limiteAnterior = limite;
        }
        return impuesto;
    }

    /**
     * Retención del mes.
     *
     * @param mesesRestantes meses por pagar del año, incluido el actual (1 a 12)
     * @param gratificacionesProyectadas gratificaciones que aún se pagarán en el año
     * @param ingresosYaPercibidos ingresos de los meses anteriores del año
     */
    public Dinero retencionMensual(Dinero remuneracionMensual, int mesesRestantes, Dinero gratificacionesProyectadas,
            Dinero ingresosYaPercibidos, ParametrosLegales parametros) {
        if (mesesRestantes < 1 || mesesRestantes > MESES_DEL_ANIO) {
            throw new IllegalArgumentException("Los meses restantes deben estar entre 1 y 12: " + mesesRestantes);
        }
        exigirNoNegativo(remuneracionMensual, "La remuneración mensual");
        exigirNoNegativo(gratificacionesProyectadas, "Las gratificaciones proyectadas");
        exigirNoNegativo(ingresosYaPercibidos, "Los ingresos ya percibidos");
        Dinero rentaBruta = remuneracionMensual.multiplicar(BigDecimal.valueOf(mesesRestantes))
                .sumar(gratificacionesProyectadas)
                .sumar(ingresosYaPercibidos);
        return impuestoAnual(rentaBruta, parametros).proporcional(1, MESES_DEL_ANIO);
    }

    private static void exigirNoNegativo(Dinero monto, String nombre) {
        if (monto.esNegativo()) {
            throw new IllegalArgumentException(nombre + " no puede ser negativa");
        }
    }
}
