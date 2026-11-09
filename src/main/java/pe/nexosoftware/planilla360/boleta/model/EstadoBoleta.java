package pe.nexosoftware.planilla360.boleta.model;

/** Ciclo de vida de una boleta: PENDIENTE → GENERADA → ENVIADA, o FALLIDA si agotó los reintentos. */
public enum EstadoBoleta {
    PENDIENTE,
    GENERADA,
    ENVIADA,
    FALLIDA
}
