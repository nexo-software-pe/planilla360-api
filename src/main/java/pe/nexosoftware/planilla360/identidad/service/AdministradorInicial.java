package pe.nexosoftware.planilla360.identidad.service;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.dto.PoliticaPassword;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.repository.UsuarioRepository;

/**
 * Crea el primer ADMIN_SAAS de un entorno nuevo, que no tiene datos de desarrollo. Es idempotente: si el
 * correo ya existe no hace nada (nunca restablece una contraseña). La contraseña nunca se escribe en el log.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicial.class);
    private static final Pattern POLITICA = Pattern.compile(PoliticaPassword.PATRON);

    private final AdministradorInicialProperties propiedades;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public AdministradorInicial(AdministradorInicialProperties propiedades, UsuarioRepository usuarios,
            PasswordEncoder encoder) {
        this.propiedades = propiedades;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        crearSiHaceFalta();
    }

    /** @return {@code true} si creó el administrador en esta llamada. */
    public boolean crearSiHaceFalta() {
        if (!propiedades.configurado()) {
            return false;
        }
        if (!POLITICA.matcher(propiedades.adminPassword()).matches()) {
            throw new IllegalStateException("ADMIN_INICIAL_PASSWORD no cumple la política: " + PoliticaPassword.MENSAJE);
        }
        String email = propiedades.adminEmail().trim();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            return false;
        }
        String nombre = propiedades.adminNombre() == null || propiedades.adminNombre().isBlank()
                ? "Administrador de la plataforma" : propiedades.adminNombre();
        try {
            usuarios.save(new Usuario(null, email, nombre, encoder.encode(propiedades.adminPassword()),
                    Rol.ADMIN_SAAS, null));
        } catch (DataIntegrityViolationException e) {
            // otra instancia lo creó al mismo tiempo: el resultado es el esperado
            return false;
        }
        LOG.info("Administrador inicial creado: {}", email);
        return true;
    }
}
