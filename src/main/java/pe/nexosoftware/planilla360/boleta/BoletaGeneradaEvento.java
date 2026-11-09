package pe.nexosoftware.planilla360.boleta;

import java.util.UUID;

/** Evento público del módulo: el PDF de una boleta quedó guardado. Dispara el aviso por correo al trabajador. */
public record BoletaGeneradaEvento(
        UUID boletaId, UUID empresaId, UUID planillaId, UUID empleadoId, String periodo, String tipo) {
}
