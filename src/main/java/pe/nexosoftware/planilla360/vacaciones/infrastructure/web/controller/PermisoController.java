package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.GestionarPermisosUseCase;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.PermisoRequest;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.PermisoResponse;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.RechazoRequest;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.mapper.VacacionesWebMapper;

@RestController
@RequestMapping("/api/v1/permisos")
@PreAuthorize("hasAnyRole('EMPLEADO', 'RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Permisos", description = "Permisos y licencias, resueltos por RRHH")
public class PermisoController {

    private final GestionarPermisosUseCase permisos;
    private final VacacionesWebMapper mapper;

    public PermisoController(GestionarPermisosUseCase permisos, VacacionesWebMapper mapper) {
        this.permisos = permisos;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PermisoResponse> solicitar(@Valid @RequestBody PermisoRequest request) {
        PermisoResponse creado = mapper.aResponse(permisos.solicitar(
                actor(), request.empleadoId(), request.tipo(), request.desde(), request.hasta(), request.conGoce(),
                request.motivo()));
        return ResponseEntity.created(URI.create("/api/v1/permisos/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<PermisoResponse> listar(@RequestParam(required = false) EstadoPermiso estado) {
        return permisos.listar(actor(), estado).stream().map(mapper::aResponse).toList();
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    public PermisoResponse aprobar(@PathVariable UUID id) {
        return mapper.aResponse(permisos.aprobar(actor(), id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    public PermisoResponse rechazar(@PathVariable UUID id, @Valid @RequestBody RechazoRequest request) {
        return mapper.aResponse(permisos.rechazar(actor(), id, request.motivo()));
    }

    private static Actor actor() {
        UsuarioAutenticado usuario = UsuarioAutenticado.requerido();
        return new Actor(usuario.usuarioId(), usuario.rol());
    }
}
