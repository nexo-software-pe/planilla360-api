package pe.nexosoftware.planilla360.planilla.application.model;

import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;

/**
 * Resultado de pedir el cierre: la planilla y si fue una repetición de un cierre ya hecho con la misma clave de
 * idempotencia (en ese caso no se hizo nada nuevo ni se publicó ningún evento).
 */
public record ResultadoCierre(Planilla planilla, boolean repetido) {
}
