package pe.nexosoftware.planilla360.compartido.dominio;

/**
 * Base de los errores de negocio lanzados por los dominios puros (sin Spring). La capa web los
 * traduce a {@code 422 problem+json}.
 */
public class ReglaDeDominioException extends RuntimeException {

    public ReglaDeDominioException(String mensaje) {
        super(mensaje);
    }
}
