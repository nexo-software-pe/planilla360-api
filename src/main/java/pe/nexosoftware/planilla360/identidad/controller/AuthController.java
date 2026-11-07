package pe.nexosoftware.planilla360.identidad.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.identidad.dto.CambiarPasswordRequest;
import pe.nexosoftware.planilla360.identidad.dto.LoginRequest;
import pe.nexosoftware.planilla360.identidad.dto.PerfilResponse;
import pe.nexosoftware.planilla360.identidad.dto.RefreshRequest;
import pe.nexosoftware.planilla360.identidad.dto.TokenResponse;
import pe.nexosoftware.planilla360.identidad.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, renovación de tokens y perfil")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return service.login(request, http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return service.refrescar(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@Valid @RequestBody RefreshRequest request) {
        service.cerrarSesion(request.refreshToken());
    }

    @GetMapping("/me")
    public PerfilResponse perfil() {
        return service.perfil();
    }

    @PostMapping("/cambiar-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request) {
        service.cambiarPassword(request);
    }
}
