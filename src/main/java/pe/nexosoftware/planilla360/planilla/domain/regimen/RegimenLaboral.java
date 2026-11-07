package pe.nexosoftware.planilla360.planilla.domain.regimen;

import java.math.BigDecimal;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/**
 * Régimen laboral de la empresa (Strategy de beneficios): define qué parte de la gratificación y la CTS se paga,
 * cuántos días de vacaciones corresponden y si se pueden vender.
 */
public enum RegimenLaboral {
    GENERAL(100, 30, true),
    PEQUENA_EMPRESA(50, 15, false);

    private final BigDecimal porcentajeBeneficios;
    private final int diasVacacionesPorAnio;
    private final boolean permiteVenderVacaciones;

    RegimenLaboral(int porcentajeBeneficios, int diasVacacionesPorAnio, boolean permiteVenderVacaciones) {
        this.porcentajeBeneficios = BigDecimal.valueOf(porcentajeBeneficios);
        this.diasVacacionesPorAnio = diasVacacionesPorAnio;
        this.permiteVenderVacaciones = permiteVenderVacaciones;
    }

    /** Gratificación y CTS: el monto completo en el régimen general, la mitad en pequeña empresa. */
    public Dinero aplicarBeneficio(Dinero montoCompleto) {
        return montoCompleto.porcentaje(porcentajeBeneficios);
    }

    public int diasVacacionesPorAnio() {
        return diasVacacionesPorAnio;
    }

    public boolean permiteVenderVacaciones() {
        return permiteVenderVacaciones;
    }
}
