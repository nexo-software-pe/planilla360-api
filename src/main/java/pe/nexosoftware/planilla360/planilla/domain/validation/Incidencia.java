package pe.nexosoftware.planilla360.planilla.domain.validation;

/** Un problema que impide cerrar la planilla: código estable para la interfaz y mensaje legible. */
public record Incidencia(String codigo, String mensaje) {
}
