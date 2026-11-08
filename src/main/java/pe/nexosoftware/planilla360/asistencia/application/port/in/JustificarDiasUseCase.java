package pe.nexosoftware.planilla360.asistencia.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface JustificarDiasUseCase {

    enum Motivo {
        VACACIONES,
        PERMISO
    }

    /** Marca como justificados todos los días de {@code desde} a {@code hasta} (ambos incluidos). */
    void justificar(UUID empleadoId, LocalDate desde, LocalDate hasta, Motivo motivo, UUID origenId);
}
