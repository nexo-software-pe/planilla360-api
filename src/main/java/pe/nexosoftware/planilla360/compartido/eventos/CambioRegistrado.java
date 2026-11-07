package pe.nexosoftware.planilla360.compartido.eventos;

import java.util.UUID;

/**
 * Evento publicado por los módulos cuando cambia una entidad sensible. El módulo de auditoría lo
 * escucha y guarda el historial. {@code antes} y {@code despues} son instantáneas serializables
 * (normalmente el DTO de respuesta, nunca datos secretos).
 */
public record CambioRegistrado(
        String entidad, UUID entidadId, UUID tenantId, Accion accion, Object antes, Object despues) {

    public enum Accion {
        CREAR,
        ACTUALIZAR,
        ELIMINAR
    }
}
