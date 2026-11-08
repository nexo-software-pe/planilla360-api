package pe.nexosoftware.planilla360.asistencia.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.asistencia.application.port.in.GestionarHorariosUseCase;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.AsignarHorarioRequest;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.HorarioRequest;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.HorarioResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.mapper.AsistenciaWebMapper;

@RestController
@RequestMapping("/api/v1/horarios")
@PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Horarios", description = "Turnos de trabajo y su asignación a empleados")
public class HorarioController {

    private final GestionarHorariosUseCase horarios;
    private final AsistenciaWebMapper mapper;

    public HorarioController(GestionarHorariosUseCase horarios, AsistenciaWebMapper mapper) {
        this.horarios = horarios;
        this.mapper = mapper;
    }

    @GetMapping
    public List<HorarioResponse> listar() {
        return horarios.listar().stream().map(mapper::aResponse).toList();
    }

    @PostMapping
    public ResponseEntity<HorarioResponse> crear(@Valid @RequestBody HorarioRequest request) {
        Horario horario = new Horario(request.entrada(), request.salida(), request.toleranciaMin(), request.dias());
        HorarioResponse creado = mapper.aResponse(horarios.crear(request.nombre(), horario));
        return ResponseEntity.created(URI.create("/api/v1/horarios/" + creado.id())).body(creado);
    }

    @PostMapping("/{id}/asignaciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignar(@PathVariable UUID id, @Valid @RequestBody AsignarHorarioRequest request) {
        horarios.asignar(id, request.empleadoId(), request.desde());
    }
}
