package pe.nexosoftware.planilla360.asistencia.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.asistencia.application.model.Actor;
import pe.nexosoftware.planilla360.asistencia.application.port.in.GestionarHorasExtraUseCase;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.HoraExtraRequest;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.HoraExtraResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.ResolverHoraExtraRequest;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.mapper.AsistenciaWebMapper;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/asistencia/horas-extra")
@PreAuthorize("hasAnyRole('EMPLEADO', 'RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Horas extra", description = "Solicitud y aprobación de horas extra (RRHH o jefe directo)")
public class HoraExtraController {

    private final GestionarHorasExtraUseCase horasExtra;
    private final AsistenciaWebMapper mapper;

    public HoraExtraController(GestionarHorasExtraUseCase horasExtra, AsistenciaWebMapper mapper) {
        this.horasExtra = horasExtra;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<HoraExtraResponse> solicitar(@Valid @RequestBody HoraExtraRequest request) {
        HoraExtraResponse creada = mapper.aResponse(horasExtra.solicitar(
                actor(), request.empleadoId(), request.fecha(), request.horas(), request.motivo()));
        return ResponseEntity.created(URI.create("/api/v1/asistencia/horas-extra/" + creada.id())).body(creada);
    }

    @GetMapping
    public List<HoraExtraResponse> listar(@RequestParam(required = false) EstadoHoraExtra estado) {
        return horasExtra.listar(actor(), estado).stream().map(mapper::aResponse).toList();
    }

    @PatchMapping("/{id}")
    public HoraExtraResponse resolver(@PathVariable UUID id, @Valid @RequestBody ResolverHoraExtraRequest request) {
        boolean aprobar = request.accion() == ResolverHoraExtraRequest.Accion.APROBAR;
        return mapper.aResponse(horasExtra.resolver(actor(), id, aprobar));
    }

    private static Actor actor() {
        UsuarioAutenticado usuario = UsuarioAutenticado.requerido();
        return new Actor(usuario.usuarioId(), usuario.rol());
    }
}
