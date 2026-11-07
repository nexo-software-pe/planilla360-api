package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** Base de los errores de negocio y de acceso que la API traduce a {@code application/problem+json}. */
public abstract class PlanillaException extends RuntimeException {

    private final HttpStatus estado;
    private final String titulo;

    protected PlanillaException(HttpStatus estado, String titulo, String detalle) {
        super(detalle);
        this.estado = estado;
        this.titulo = titulo;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String titulo() {
        return titulo;
    }
}
