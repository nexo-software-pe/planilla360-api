package pe.nexosoftware.planilla360.vacaciones.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/** Un año de servicio del empleado y sus días de vacaciones. (Esqueleto del commit ROJO.) */
public final class PeriodoVacacional {

    private final UUID id;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final int diasGanados;
    private int diasGozados;
    private int diasVendidos;

    public PeriodoVacacional(
            UUID id, LocalDate desde, LocalDate hasta, int diasGanados, int diasGozados, int diasVendidos) {
        throw new UnsupportedOperationException("pendiente");
    }

    public int saldo() {
        throw new UnsupportedOperationException("pendiente");
    }

    public void registrarGoce(int dias) {
        throw new UnsupportedOperationException("pendiente");
    }

    public void vender(int dias, PoliticaVacaciones politica) {
        throw new UnsupportedOperationException("pendiente");
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
