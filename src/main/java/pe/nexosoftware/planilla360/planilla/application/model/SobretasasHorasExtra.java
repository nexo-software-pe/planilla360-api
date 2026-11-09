package pe.nexosoftware.planilla360.planilla.application.model;

import java.math.BigDecimal;

/** Sobretasas vigentes de las horas extra, en porcentaje (25 = 25 %). */
public record SobretasasHorasExtra(BigDecimal primerasDosHoras, BigDecimal desdeLaTercera) {
}
