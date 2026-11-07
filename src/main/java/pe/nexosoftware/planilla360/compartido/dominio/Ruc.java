package pe.nexosoftware.planilla360.compartido.dominio;

/** RUC peruano: 11 dígitos, prefijo de contribuyente válido y dígito verificador módulo 11. */
public record Ruc(String valor) {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    public Ruc {
        if (!esValido(valor)) {
            throw new IllegalArgumentException("RUC inválido: " + valor);
        }
    }

    public static boolean esValido(String valor) {
        if (valor == null || !valor.matches("\\d{11}")) {
            return false;
        }
        String prefijo = valor.substring(0, 2);
        if (!prefijo.equals("10") && !prefijo.equals("15") && !prefijo.equals("17") && !prefijo.equals("20")) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += Character.getNumericValue(valor.charAt(i)) * PESOS[i];
        }
        int resto = 11 - (suma % 11);
        int digito = resto == 10 ? 0 : resto == 11 ? 1 : resto;
        return digito == Character.getNumericValue(valor.charAt(10));
    }

    @Override
    public String toString() {
        return valor;
    }
}
