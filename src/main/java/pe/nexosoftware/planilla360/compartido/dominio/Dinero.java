package pe.nexosoftware.planilla360.compartido.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * Monto en soles. Siempre con dos decimales y redondeo mitad hacia arriba; inmutable.
 * Se usa {@link BigDecimal} (nunca {@code double}) para no arrastrar errores binarios.
 */
public record Dinero(BigDecimal monto) implements Comparable<Dinero> {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        monto = monto.setScale(ESCALA, REDONDEO);
    }

    public static Dinero de(String valor) {
        return new Dinero(new BigDecimal(valor));
    }

    public static Dinero de(BigDecimal valor) {
        return new Dinero(valor);
    }

    public static Dinero de(long soles) {
        return new Dinero(BigDecimal.valueOf(soles));
    }

    public static Dinero suma(Collection<Dinero> montos) {
        Dinero total = CERO;
        for (Dinero m : montos) {
            total = total.sumar(m);
        }
        return total;
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(monto.add(otro.monto));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(monto.subtract(otro.monto));
    }

    public Dinero multiplicar(BigDecimal factor) {
        return new Dinero(monto.multiply(factor));
    }

    /** Calcula {@code porcentaje} % del monto (por ejemplo 13 para el 13 %), redondeando una sola vez. */
    public Dinero porcentaje(BigDecimal porcentaje) {
        return new Dinero(dividir(monto.multiply(porcentaje), CIEN));
    }

    /** Calcula {@code monto x numerador / denominador}: multiplica primero y redondea una sola vez. */
    public Dinero proporcional(long numerador, long denominador) {
        if (denominador <= 0) {
            throw new IllegalArgumentException("El denominador debe ser positivo: " + denominador);
        }
        return new Dinero(dividir(monto.multiply(BigDecimal.valueOf(numerador)), BigDecimal.valueOf(denominador)));
    }

    private static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        return dividendo.divide(divisor, ESCALA, REDONDEO);
    }

    public Dinero max(Dinero otro) {
        return new Dinero(monto.max(otro.monto));
    }

    public Dinero min(Dinero otro) {
        return new Dinero(monto.min(otro.monto));
    }

    public boolean esNegativo() {
        return monto.signum() < 0;
    }

    public boolean esCero() {
        return monto.signum() == 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return monto.compareTo(otro.monto);
    }

    @Override
    public String toString() {
        return monto.toPlainString();
    }
}
