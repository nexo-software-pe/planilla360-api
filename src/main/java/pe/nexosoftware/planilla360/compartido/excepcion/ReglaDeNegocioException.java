package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ReglaDeNegocioException extends PlanillaException {

    public ReglaDeNegocioException(String detalle) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio incumplida", detalle);
    }
}
