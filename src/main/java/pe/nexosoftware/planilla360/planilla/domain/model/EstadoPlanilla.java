package pe.nexosoftware.planilla360.planilla.domain.model;

/** Ciclo de vida de una planilla (patrón State). Esqueleto: se construye test-first. */
public enum EstadoPlanilla {
    BORRADOR,
    CALCULADA,
    CERRADA,
    ANULADA;

    public EstadoPlanilla alCalcular() {
        throw new UnsupportedOperationException("pendiente");
    }

    public EstadoPlanilla alCerrar() {
        throw new UnsupportedOperationException("pendiente");
    }

    public EstadoPlanilla alAnular() {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean permiteModificarLineas() {
        throw new UnsupportedOperationException("pendiente");
    }
}
