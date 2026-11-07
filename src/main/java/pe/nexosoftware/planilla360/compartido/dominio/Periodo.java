package pe.nexosoftware.planilla360.compartido.dominio;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/** Mes calendario de una planilla (por ejemplo 2026-07). Inmutable y ordenable. */
public record Periodo(YearMonth valor) implements Comparable<Periodo> {

    public Periodo {
        Objects.requireNonNull(valor, "El periodo es obligatorio");
    }

    public static Periodo de(int anio, int mes) {
        try {
            return new Periodo(YearMonth.of(anio, mes));
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Periodo inválido: " + anio + "-" + mes, e);
        }
    }

    /** Interpreta el formato {@code yyyy-MM}. */
    public static Periodo parse(String texto) {
        Objects.requireNonNull(texto, "El periodo es obligatorio");
        try {
            return new Periodo(YearMonth.parse(texto));
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Periodo inválido (se espera yyyy-MM): " + texto, e);
        }
    }

    public static Periodo de(LocalDate fecha) {
        return new Periodo(YearMonth.from(fecha));
    }

    public int anio() {
        return valor.getYear();
    }

    public int mes() {
        return valor.getMonthValue();
    }

    public LocalDate inicio() {
        return valor.atDay(1);
    }

    public LocalDate fin() {
        return valor.atEndOfMonth();
    }

    public int diasDelMes() {
        return valor.lengthOfMonth();
    }

    public Periodo anterior() {
        return new Periodo(valor.minusMonths(1));
    }

    public Periodo siguiente() {
        return new Periodo(valor.plusMonths(1));
    }

    public boolean contiene(LocalDate fecha) {
        return YearMonth.from(fecha).equals(valor);
    }

    @Override
    public int compareTo(Periodo otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
