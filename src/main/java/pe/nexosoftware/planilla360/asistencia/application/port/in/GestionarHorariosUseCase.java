package pe.nexosoftware.planilla360.asistencia.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HorarioPort.HorarioGuardado;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;

public interface GestionarHorariosUseCase {

    HorarioGuardado crear(String nombre, Horario horario);

    List<HorarioGuardado> listar();

    void asignar(UUID horarioId, UUID empleadoId, LocalDate desde);
}
