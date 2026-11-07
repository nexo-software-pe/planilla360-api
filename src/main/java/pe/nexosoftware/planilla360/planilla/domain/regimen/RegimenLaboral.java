package pe.nexosoftware.planilla360.planilla.domain.regimen;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Régimen laboral de la empresa (Strategy de beneficios). Esqueleto: se construye test-first. */
public enum RegimenLaboral {
    GENERAL,
    PEQUENA_EMPRESA;

    public Dinero aplicarBeneficio(Dinero montoCompleto) {
        throw new UnsupportedOperationException("pendiente");
    }

    public int diasVacacionesPorAnio() {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean permiteVenderVacaciones() {
        throw new UnsupportedOperationException("pendiente");
    }
}
