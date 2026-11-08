package pe.nexosoftware.planilla360.asistencia.application.port.in;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

public interface RegistrarMarcacionUseCase {

    record MarcacionRegistrada(TipoMarcacion tipo, Instant instante) {
    }

    /** Registra la marcación (entrada o salida, según corresponda) de la cuenta de usuario indicada. */
    MarcacionRegistrada registrar(UUID usuarioId);
}
