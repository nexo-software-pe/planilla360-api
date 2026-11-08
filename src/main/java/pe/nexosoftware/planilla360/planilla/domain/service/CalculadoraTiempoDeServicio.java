package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.LocalDate;
import java.time.Period;
import pe.nexosoftware.planilla360.planilla.domain.model.SemestreDeBeneficio;
import pe.nexosoftware.planilla360.planilla.domain.model.TiempoComputable;

/** Meses y días completos que un trabajador computa en un semestre, según su ingreso y su cese. */
public final class CalculadoraTiempoDeServicio {

    private static final int DIAS_POR_MES_COMERCIAL = 30;

    /**
     * @param ingreso fecha de ingreso del trabajador
     * @param cese fecha de cese, o {@code null} si sigue vigente
     */
    public TiempoComputable computar(LocalDate ingreso, LocalDate cese, SemestreDeBeneficio semestre) {
        LocalDate desde = ingreso.isAfter(semestre.inicio()) ? ingreso : semestre.inicio();
        LocalDate hasta = cese != null && cese.isBefore(semestre.fin()) ? cese : semestre.fin();
        if (desde.isAfter(hasta)) {
            return new TiempoComputable(0, 0);
        }
        Period transcurrido = Period.between(desde, hasta.plusDays(1));
        int meses = (int) transcurrido.toTotalMonths();
        int dias = transcurrido.getDays();
        if (dias >= DIAS_POR_MES_COMERCIAL) {
            meses++;
            dias -= DIAS_POR_MES_COMERCIAL;
        }
        return new TiempoComputable(meses, dias);
    }
}
