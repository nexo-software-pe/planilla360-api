package pe.nexosoftware.planilla360.identidad.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Administrador de la plataforma que se crea al arrancar en un entorno nuevo (staging, producción).
 * Si el correo o la contraseña no se definen, no se crea nada. Los valores llegan por variables de
 * entorno ({@code ADMIN_INICIAL_EMAIL}, {@code ADMIN_INICIAL_PASSWORD}, {@code ADMIN_INICIAL_NOMBRE}).
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record AdministradorInicialProperties(String adminEmail, String adminPassword, String adminNombre) {

    public boolean configurado() {
        return adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank();
    }
}
