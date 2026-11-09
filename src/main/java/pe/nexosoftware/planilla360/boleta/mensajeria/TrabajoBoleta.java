package pe.nexosoftware.planilla360.boleta.mensajeria;

import java.util.UUID;

/** Mensaje de la cola de generación: una boleta por trabajador. Lleva la empresa porque el consumidor no tiene JWT. */
public record TrabajoBoleta(UUID empresaId, UUID planillaId, UUID empleadoId) {
}
