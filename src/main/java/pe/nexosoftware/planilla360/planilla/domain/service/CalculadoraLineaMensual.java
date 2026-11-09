package pe.nexosoftware.planilla360.planilla.domain.service;

import java.math.BigDecimal;
import java.util.List;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosMensualesTrabajador;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;

/**
 * Template Method del cálculo mensual de un trabajador: básico proporcional a los días laborados, asignación
 * familiar, horas extra, descuentos de asistencia, pensión (según su estrategia) y retención de 5.ª categoría.
 * La gratificación y la CTS no entran aquí: se pagan en sus propias planillas.
 */
public final class CalculadoraLineaMensual {

    private static final int MESES_DEL_ANIO = 12;
    private static final int JULIO = 7;
    private static final int GRATIFICACIONES_ANTES_DE_JULIO = 2;
    private static final int MESES_DEL_SEMESTRE = 6;

    private final CalculadoraAsignacionFamiliar asignacionFamiliar = new CalculadoraAsignacionFamiliar();
    private final CalculadoraHorasExtra horasExtra = new CalculadoraHorasExtra();
    private final CalculadoraDescuentoAsistencia descuentoAsistencia = new CalculadoraDescuentoAsistencia();
    private final CalculadoraGratificacion gratificacion = new CalculadoraGratificacion();
    private final CalculadoraRenta5ta renta5ta = new CalculadoraRenta5ta();

    /**
     * @param sobretasaHoraExtra1Pct sobretasa de las dos primeras horas extra del día (25)
     * @param sobretasaHoraExtra2Pct sobretasa de las horas siguientes (35)
     */
    public LineaPlanilla calcular(DatosMensualesTrabajador datos, Periodo periodo, ParametrosLegales parametros,
            BigDecimal sobretasaHoraExtra1Pct, BigDecimal sobretasaHoraExtra2Pct) {
        exigirDiasValidos(datos.diasLaborados(), periodo);
        int horasJornada = datos.horasJornadaDiaria();
        Dinero basicoDelMes = datos.sueldoBasico().proporcional(datos.diasLaborados(), periodo.diasDelMes());
        Dinero asignacion = asignacionFamiliar.calcular(datos.hijos(), periodo.fin(), parametros);
        Dinero pagoHorasExtra = horasExtra.total(datos.sueldoBasico(), horasJornada, datos.horasExtraPorDia(),
                sobretasaHoraExtra1Pct, sobretasaHoraExtra2Pct);
        Dinero descuento = descuentoAsistencia
                .total(datos.sueldoBasico(), datos.minutosTardanza(), datos.diasInasistencia(), horasJornada)
                .min(basicoDelMes);
        Dinero retencion = retencionDeQuinta(datos, asignacion, periodo, parametros);
        return LineaPlanilla.de(datos.empleadoId())
                .basico(basicoDelMes)
                .asignacionFamiliar(asignacion)
                .horasExtra(pagoHorasExtra)
                .regimenPensionario(datos.regimenPensionario())
                .retencion5ta(retencion)
                .descuentoAsistencia(descuento)
                .build(parametros);
    }

    /**
     * La retención proyecta el año con la remuneración fija del trabajador (básico y asignación familiar), las
     * gratificaciones que aún se pagarán (dos antes de julio, una después) y lo ya percibido.
     */
    private Dinero retencionDeQuinta(DatosMensualesTrabajador datos, Dinero asignacion, Periodo periodo,
            ParametrosLegales parametros) {
        Dinero remuneracionFija = datos.sueldoBasico().sumar(asignacion);
        Dinero porGratificacion = gratificacion
                .calcular(datos.sueldoBasico(), asignacion, List.of(), MESES_DEL_SEMESTRE,
                        datos.regimenLaboral(), parametros)
                .total();
        int pendientes = periodo.mes() <= JULIO ? GRATIFICACIONES_ANTES_DE_JULIO : 1;
        Dinero proyectadas = porGratificacion.multiplicar(BigDecimal.valueOf(pendientes));
        int mesesRestantes = MESES_DEL_ANIO - periodo.mes() + 1;
        return renta5ta.retencionMensual(remuneracionFija, mesesRestantes, proyectadas, datos.ingresosYaPercibidos(),
                parametros);
    }

    private static void exigirDiasValidos(int diasLaborados, Periodo periodo) {
        if (diasLaborados < 1 || diasLaborados > periodo.diasDelMes()) {
            throw new IllegalArgumentException("Los días laborados deben estar entre 1 y " + periodo.diasDelMes()
                    + " en " + periodo + ": " + diasLaborados);
        }
    }
}
