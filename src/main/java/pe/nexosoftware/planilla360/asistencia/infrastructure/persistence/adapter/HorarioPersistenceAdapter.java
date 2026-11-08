package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.adapter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HorarioPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.EmpleadoHorarioEntity;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity.HorarioEntity;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository.EmpleadoHorarioJpaRepository;
import pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.repository.HorarioJpaRepository;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

@Component
class HorarioPersistenceAdapter implements HorarioPort {

    private final HorarioJpaRepository horarios;
    private final EmpleadoHorarioJpaRepository asignaciones;

    HorarioPersistenceAdapter(HorarioJpaRepository horarios, EmpleadoHorarioJpaRepository asignaciones) {
        this.horarios = horarios;
        this.asignaciones = asignaciones;
    }

    @Override
    public HorarioGuardado guardar(String nombre, Horario horario) {
        HorarioEntity entidad = horarios.saveAndFlush(new HorarioEntity(
                nombre, horario.entrada(), horario.salida(), horario.toleranciaMin(), serializarDias(horario.dias())));
        return aGuardado(entidad);
    }

    @Override
    public List<HorarioGuardado> listar() {
        return horarios.findAllByOrderByNombreAsc().stream()
                .map(HorarioPersistenceAdapter::aGuardado)
                .toList();
    }

    @Override
    public boolean existe(UUID horarioId) {
        return horarios.existsById(horarioId);
    }

    @Override
    public boolean existeNombre(String nombre) {
        return horarios.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public void asignar(UUID empleadoId, UUID horarioId, LocalDate desde) {
        asignaciones.findByEmpleadoIdAndHastaIsNull(empleadoId).ifPresent(abierta -> {
            if (!abierta.getDesde().isBefore(desde)) {
                throw new ReglaDeNegocioException(
                        "La nueva asignación debe empezar después del " + abierta.getDesde() + ".");
            }
            abierta.cerrarEl(desde.minusDays(1));
            asignaciones.flush();
        });
        asignaciones.saveAndFlush(new EmpleadoHorarioEntity(empleadoId, horarioId, desde));
    }

    @Override
    public Map<UUID, Horario> vigentesEn(LocalDate fecha) {
        List<EmpleadoHorarioEntity> vigentes = asignaciones.findVigentesEn(fecha);
        Map<UUID, Horario> porHorario = new HashMap<>();
        horarios.findAllById(vigentes.stream().map(EmpleadoHorarioEntity::getHorarioId).distinct().toList())
                .forEach(h -> porHorario.put(h.getId(), aDominio(h)));
        Map<UUID, Horario> porEmpleado = new HashMap<>();
        vigentes.forEach(a -> porEmpleado.put(a.getEmpleadoId(), porHorario.get(a.getHorarioId())));
        return porEmpleado;
    }

    private static HorarioGuardado aGuardado(HorarioEntity entidad) {
        return new HorarioGuardado(entidad.getId(), entidad.getNombre(), aDominio(entidad));
    }

    private static Horario aDominio(HorarioEntity entidad) {
        return new Horario(
                entidad.getHoraEntrada(),
                entidad.getHoraSalida(),
                entidad.getToleranciaMin(),
                deserializarDias(entidad.getDias()));
    }

    private static String serializarDias(Set<DayOfWeek> dias) {
        return EnumSet.copyOf(dias).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private static Set<DayOfWeek> deserializarDias(String dias) {
        return Arrays.stream(dias.split(",")).map(DayOfWeek::valueOf).collect(Collectors.toCollection(
                () -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
