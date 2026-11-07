package pe.nexosoftware.planilla360.planilla.domain.model;

import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

/** Resultado del cálculo de la CTS de un semestre: la remuneración usada y el monto a depositar. */
public record DepositoCts(Dinero remuneracionComputable, Dinero monto) {
}
