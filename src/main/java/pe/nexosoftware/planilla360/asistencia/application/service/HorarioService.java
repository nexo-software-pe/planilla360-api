package pe.nexosoftware.planilla360.asistencia.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.asistencia.application.port.in.GestionarHorariosUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HorarioPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HorarioPort.HorarioGuardado;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;

@Service
@Transactional(readOnly = true)
public class HorarioService implements GestionarHorariosUseCase {

    private final HorarioPort horarios;
    private final ConsultaEmpleadoPort empleados;

    public HorarioService(HorarioPort horarios, ConsultaEmpleadoPort empleados) {
        this.horarios = horarios;
        this.empleados = empleados;
    }

    @Override
    @Transactional
    public HorarioGuardado crear(String nombre, Horario horario) {
        if (horarios.existeNombre(nombre.trim())) {
            throw new ConflictoException("Ya existe un horario con ese nombre.");
        }
        return horarios.guardar(nombre.trim(), horario);
    }

    @Override
    public List<HorarioGuardado> listar() {
        return horarios.listar();
    }

    @Override
    @Transactional
    public void asignar(UUID horarioId, UUID empleadoId, LocalDate desde) {
        if (!horarios.existe(horarioId)) {
            throw new RecursoNoEncontradoException("No existe el horario solicitado.");
        }
        if (empleados.buscar(empleadoId).isEmpty()) {
            throw new RecursoNoEncontradoException("No existe el empleado solicitado.");
        }
        horarios.asignar(empleadoId, horarioId, desde);
    }
}
