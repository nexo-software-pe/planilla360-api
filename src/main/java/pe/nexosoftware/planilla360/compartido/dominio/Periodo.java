package pe.nexosoftware.planilla360.compartido.dominio;

import java.time.LocalDate;
import java.time.YearMonth;

/** Mes calendario de una planilla (por ejemplo 2026-07). Esqueleto: se construye test-first. */
public record Periodo(YearMonth valor) implements Comparable<Periodo> {

    public static Periodo de(int anio, int mes) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static Periodo parse(String texto) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static Periodo de(LocalDate fecha) {
        throw new UnsupportedOperationException("pendiente");
    }

    public int anio() {
        throw new UnsupportedOperationException("pendiente");
    }

    public int mes() {
        throw new UnsupportedOperationException("pendiente");
    }

    public LocalDate inicio() {
        throw new UnsupportedOperationException("pendiente");
    }

    public LocalDate fin() {
        throw new UnsupportedOperationException("pendiente");
    }

    public int diasDelMes() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Periodo anterior() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Periodo siguiente() {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean contiene(LocalDate fecha) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int compareTo(Periodo otro) {
        throw new UnsupportedOperationException("pendiente");
    }
}
