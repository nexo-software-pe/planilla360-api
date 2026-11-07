package pe.nexosoftware.planilla360.identidad.dto;

/** Política de contraseñas: 10 a 72 caracteres (límite de BCrypt) con al menos una letra y un dígito. */
public final class PoliticaPassword {

    public static final String PATRON = "^(?=.*[A-Za-z])(?=.*\\d).{10,72}$";
    public static final String MENSAJE = "La contraseña debe tener entre 10 y 72 caracteres, con letras y números";

    private PoliticaPassword() {
    }
}
