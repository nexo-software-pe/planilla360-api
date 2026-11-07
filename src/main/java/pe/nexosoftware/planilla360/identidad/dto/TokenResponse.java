package pe.nexosoftware.planilla360.identidad.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiraEnSegundos,
        UsuarioResponse usuario) {
}
