package pe.nexosoftware.planilla360.compartido.dominio;

/** Documento Nacional de Identidad: 8 dígitos. */
public record Dni(String valor) {

    public Dni {
        if (!esValido(valor)) {
            throw new IllegalArgumentException("DNI inválido: " + valor);
        }
    }

    public static boolean esValido(String valor) {
        return valor != null && valor.matches("\\d{8}");
    }

    @Override
    public String toString() {
        return valor;
    }
}
