package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends PlanillaException {

    public RecursoNoEncontradoException(String detalle) {
        super(HttpStatus.NOT_FOUND, "Recurso no encontrado", detalle);
    }
}
