package pe.nexosoftware.planilla360.soporte;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.empresa.repository.EmpresaRepository;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.repository.UsuarioRepository;
import pe.nexosoftware.planilla360.identidad.service.TokenService;
import tools.jackson.databind.json.JsonMapper;

/** Base de las pruebas de integración: aplicación completa + PostgreSQL real + MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegracionBase {

    protected static final String CLAVE_PRUEBA = "Clave-Segura-123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper json;

    @Autowired
    protected EmpresaRepository empresas;

    @Autowired
    protected UsuarioRepository usuarios;

    @Autowired
    protected PasswordEncoder encoder;

    @Autowired
    protected TokenService tokens;

    protected Empresa nuevaEmpresa() {
        String ruc = RucDePrueba.nuevo();
        return empresas.save(new Empresa(ruc, "Empresa de prueba " + ruc, RegimenLaboral.GENERAL, PlanSaas.PROFESIONAL));
    }

    protected Usuario nuevoUsuario(Empresa empresa, Rol rol) {
        UUID tenant = rol == Rol.ADMIN_SAAS ? null : empresa.getId();
        String email = rol.name().toLowerCase() + "." + UUID.randomUUID() + "@ejemplo.pe";
        return usuarios.save(new Usuario(tenant, email, "Usuario " + rol, encoder.encode(CLAVE_PRUEBA), rol, null));
    }

    protected String bearer(Usuario usuario) {
        return "Bearer " + tokens.crearAccessToken(usuario);
    }
}
