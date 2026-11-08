package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.GestionarVacacionesUseCase;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.GestionarVacacionesUseCase.SaldoVacaciones;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.RechazoRequest;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.SolicitudVacacionesRequest;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.SolicitudVacacionesResponse;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.VentaDiasRequest;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.mapper.VacacionesWebMapper;

@RestController
@RequestMapping("/api/v1/vacaciones")
@PreAuthorize("hasAnyRole('EMPLEADO', 'RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Vacaciones", description = "Saldo, solicitudes y aprobación (jefe directo y luego RRHH)")
public class VacacionesController {

    private final GestionarVacacionesUseCase vacaciones;
    private final VacacionesWebMapper mapper;

    public VacacionesController(GestionarVacacionesUseCase vacaciones, VacacionesWebMapper mapper) {
        this.vacaciones = vacaciones;
        this.mapper = mapper;
    }

    @GetMapping("/saldo/mio")
    public SaldoVacaciones miSaldo() {
        return vacaciones.miSaldo(UsuarioAutenticado.requerido().usuarioId());
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudVacacionesResponse> solicitar(@Valid @RequestBody SolicitudVacacionesRequest request) {
        SolicitudVacacionesResponse creada =
                mapper.aResponse(vacaciones.solicitar(actor(), request.desde(), request.hasta()));
        return ResponseEntity.created(URI.create("/api/v1/vacaciones/solicitudes/" + creada.id())).body(creada);
    }

    @GetMapping("/solicitudes")
    public List<SolicitudVacacionesResponse> listar(
            @RequestParam(required = false) EstadoSolicitud estado, @RequestParam(required = false) UUID empleadoId) {
        return vacaciones.listar(actor(), estado, empleadoId).stream().map(mapper::aResponse).toList();
    }

    @PostMapping("/solicitudes/{id}/aprobar-jefe")
    public SolicitudVacacionesResponse aprobarJefe(@PathVariable UUID id) {
        return mapper.aResponse(vacaciones.aprobarPorJefe(actor(), id));
    }

    @PostMapping("/solicitudes/{id}/aprobar-rrhh")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    public SolicitudVacacionesResponse aprobarRrhh(@PathVariable UUID id) {
        return mapper.aResponse(vacaciones.aprobarPorRrhh(actor(), id));
    }

    @PostMapping("/solicitudes/{id}/rechazar")
    public SolicitudVacacionesResponse rechazar(@PathVariable UUID id, @Valid @RequestBody RechazoRequest request) {
        return mapper.aResponse(vacaciones.rechazar(actor(), id, request.motivo()));
    }

    @PostMapping("/solicitudes/{id}/cancelar")
    public SolicitudVacacionesResponse cancelar(@PathVariable UUID id) {
        return mapper.aResponse(vacaciones.cancelar(actor(), id));
    }

    @PostMapping("/periodos/{id}/venta")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vender(@PathVariable UUID id, @Valid @RequestBody VentaDiasRequest request) {
        vacaciones.venderDias(actor(), id, request.dias());
    }

    private static Actor actor() {
        UsuarioAutenticado usuario = UsuarioAutenticado.requerido();
        return new Actor(usuario.usuarioId(), usuario.rol());
    }
}
