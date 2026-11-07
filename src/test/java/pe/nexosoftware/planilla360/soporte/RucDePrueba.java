package pe.nexosoftware.planilla360.soporte;

import java.util.concurrent.ThreadLocalRandom;

/** Genera RUC con dígito verificador válido para no chocar entre pruebas. */
public final class RucDePrueba {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private RucDePrueba() {
    }

    public static String nuevo() {
        StringBuilder base = new StringBuilder("20");
        for (int i = 0; i < 8; i++) {
            base.append(ThreadLocalRandom.current().nextInt(10));
        }
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += Character.getNumericValue(base.charAt(i)) * PESOS[i];
        }
        int resto = 11 - (suma % 11);
        int digito = resto == 10 ? 0 : resto == 11 ? 1 : resto;
        return base.append(digito).toString();
    }
}
