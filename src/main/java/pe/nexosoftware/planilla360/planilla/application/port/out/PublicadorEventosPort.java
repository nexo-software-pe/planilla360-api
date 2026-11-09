package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.planilla.domain.event.PlanillaCerrada;

/** Publica los eventos del módulo para que otros módulos reaccionen y para el historial de auditoría. */
public interface PublicadorEventosPort {

    void publicar(PlanillaCerrada evento);

    /** Registra un cambio en el historial de auditoría (las instantáneas no deben incluir datos secretos). */
    void registrarCambio(UUID planillaId, Accion accion, Object antes, Object despues);
}
