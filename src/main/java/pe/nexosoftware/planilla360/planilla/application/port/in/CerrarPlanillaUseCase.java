package pe.nexosoftware.planilla360.planilla.application.port.in;

import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.application.model.ResultadoCierre;

public interface CerrarPlanillaUseCase {

    /**
     * Cierra la planilla de forma idempotente: repetir la petición con la misma clave devuelve la misma planilla
     * cerrada sin volver a cerrar ni a publicar el evento.
     */
    ResultadoCierre cerrar(UUID planillaId, String claveIdempotencia);
}
