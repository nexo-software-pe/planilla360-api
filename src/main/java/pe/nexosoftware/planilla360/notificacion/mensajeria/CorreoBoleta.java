package pe.nexosoftware.planilla360.notificacion.mensajeria;

import java.util.UUID;

/** Mensaje de la cola de correos: avisar al trabajador que su boleta está disponible. */
public record CorreoBoleta(UUID empresaId, UUID boletaId, UUID empleadoId, String periodo, String tipo) {
}
