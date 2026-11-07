package pe.nexosoftware.planilla360.planilla.domain.service;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/**
 * Descuentos por tardanzas (valor minuto) e inasistencias injustificadas (valor día).
 * El mes comercial es de 30 días; el descuento nunca supera el sueldo básico.
 */
public final class CalculadoraDescuentoAsistencia {

    private static final int DIAS_COMERCIALES = 30;
    private static final int MINUTOS_POR_HORA = 60;

    public Dinero porTardanza(Dinero basico, int minutosTardanza, int horasJornadaDiaria) {
        exigirBasicoValido(basico);
        if (minutosTardanza < 0) {
            throw new IllegalArgumentException("Los minutos de tardanza no pueden ser negativos");
        }
        if (horasJornadaDiaria <= 0) {
            throw new IllegalArgumentException("La jornada diaria debe ser positiva");
        }
        long minutosDelMes = (long) DIAS_COMERCIALES * horasJornadaDiaria * MINUTOS_POR_HORA;
        return basico.proporcional(minutosTardanza, minutosDelMes).min(basico);
    }

    public Dinero porInasistencias(Dinero basico, int diasInjustificados) {
        exigirBasicoValido(basico);
        if (diasInjustificados < 0) {
            throw new IllegalArgumentException("Los días de inasistencia no pueden ser negativos");
        }
        return basico.proporcional(diasInjustificados, DIAS_COMERCIALES).min(basico);
    }

    public Dinero total(Dinero basico, int minutosTardanza, int diasInjustificados, int horasJornadaDiaria) {
        Dinero suma = porTardanza(basico, minutosTardanza, horasJornadaDiaria)
                .sumar(porInasistencias(basico, diasInjustificados));
        return suma.min(basico);
    }

    private static void exigirBasicoValido(Dinero basico) {
        if (basico.esNegativo()) {
            throw new IllegalArgumentException("El sueldo básico no puede ser negativo");
        }
    }
}
