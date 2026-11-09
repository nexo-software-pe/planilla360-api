package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** Un servicio externo no responde (o su cortacircuito está abierto). Se responde 503. */
public class ServicioNoDisponibleException extends PlanillaException {

    public ServicioNoDisponibleException(String detalle) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Servicio externo no disponible", detalle);
    }
}
