package pe.nexosoftware.planilla360.vacaciones.domain.model;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.excepcion.ReglaDeVacacionesException;

/**
 * Un año de servicio del empleado y sus días de vacaciones. El saldo es lo ganado menos lo gozado y lo
 * vendido, y nunca puede quedar negativo.
 */
public final class PeriodoVacacional {

    private final UUID id;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final int diasGanados;
    private int diasGozados;
    private int diasVendidos;

    public PeriodoVacacional(
            UUID id, LocalDate desde, LocalDate hasta, int diasGanados, int diasGozados, int diasVendidos) {
        if (diasGanados < 0 || diasGozados < 0 || diasVendidos < 0) {
            throw new ReglaDeVacacionesException("Los días del periodo no pueden ser negativos.");
        }
        if (diasGozados + diasVendidos > diasGanados) {
            throw new ReglaDeVacacionesException("Los días gozados y vendidos superan los días ganados.");
        }
        this.id = id;
        this.desde = desde;
        this.hasta = hasta;
        this.diasGanados = diasGanados;
        this.diasGozados = diasGozados;
        this.diasVendidos = diasVendidos;
    }

    public int saldo() {
        return diasGanados - diasGozados - diasVendidos;
    }

    public void registrarGoce(int dias) {
        exigirDiasValidos(dias);
        this.diasGozados += dias;
    }

    public void vender(int dias, PoliticaVacaciones politica) {
        if (politica.maximoDiasVendibles() == 0) {
            throw new ReglaDeVacacionesException("El régimen laboral de la empresa no permite vender vacaciones.");
        }
        exigirDiasValidos(dias);
        if (diasVendidos + dias > politica.maximoDiasVendibles()) {
            throw new ReglaDeVacacionesException(
                    "Solo se pueden vender hasta " + politica.maximoDiasVendibles() + " días por periodo.");
        }
        this.diasVendidos += dias;
    }

    private void exigirDiasValidos(int dias) {
        if (dias <= 0) {
            throw new ReglaDeVacacionesException("La cantidad de días debe ser mayor a cero.");
        }
        if (dias > saldo()) {
            throw new ReglaDeVacacionesException("El saldo de vacaciones (" + saldo() + " días) no alcanza.");
        }
    }

    public UUID id() {
        return id;
    }

    public LocalDate desde() {
        return desde;
    }

    public LocalDate hasta() {
        return hasta;
    }

    public int diasGanados() {
        return diasGanados;
    }

    public int diasGozados() {
        return diasGozados;
    }

    public int diasVendidos() {
        return diasVendidos;
    }
}
