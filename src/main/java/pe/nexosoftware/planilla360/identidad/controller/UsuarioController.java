package pe.nexosoftware.planilla360.identidad.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.identidad.dto.ActualizarUsuarioRequest;
import pe.nexosoftware.planilla360.identidad.dto.EstadoUsuarioRequest;
import pe.nexosoftware.planilla360.identidad.dto.ResetPasswordRequest;
import pe.nexosoftware.planilla360.identidad.dto.UsuarioRequest;
import pe.nexosoftware.planilla360.identidad.dto.UsuarioResponse;
import pe.nexosoftware.planilla360.identidad.service.UsuarioService;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasAnyRole('ADMIN_SAAS', 'ADMIN_EMPRESA')")
@Tag(name = "Usuarios", description = "Administración de usuarios de la empresa")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<UsuarioResponse> listar(
            @RequestParam(required = false) UUID tenantId,
            @PageableDefault(size = 20, sort = "email") Pageable pageable) {
        return service.listar(tenantId, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody EstadoUsuarioRequest request) {
        return service.cambiarEstado(id, request.activo());
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetearPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        service.resetearPassword(id, request);
    }
}
