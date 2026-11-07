package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class CuentaBloqueadaException extends PlanillaException {

    public CuentaBloqueadaException(String detalle) {
        super(HttpStatus.LOCKED, "Cuenta bloqueada temporalmente", detalle);
    }
}
