package pe.nexosoftware.planilla360.compartido.dominio;

import java.math.BigDecimal;
import java.util.Collection;

/** Monto en soles. Esqueleto: el comportamiento se construye test-first. */
public record Dinero(BigDecimal monto) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public static Dinero de(String valor) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static Dinero de(BigDecimal valor) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static Dinero de(long soles) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static Dinero suma(Collection<Dinero> montos) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero sumar(Dinero otro) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero restar(Dinero otro) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero multiplicar(BigDecimal factor) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero porcentaje(BigDecimal porcentaje) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero proporcional(long numerador, long denominador) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero max(Dinero otro) {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero min(Dinero otro) {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean esNegativo() {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean esCero() {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int compareTo(Dinero otro) {
        throw new UnsupportedOperationException("pendiente");
    }
}
