package pe.nexosoftware.planilla360.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;

/** Apoyo para pruebas de integración que necesitan empresa, áreas, cargos y empleados reales. */
@Import(RelojDePruebaConfig.class)
public abstract class PersonalBase extends IntegracionBase {

    @Autowired
    protected RelojDePrueba reloj;

    @AfterEach
    void restablecerReloj() {
        reloj.restablecer();
    }

    protected void ahoraEs(String fechaHoraLima) {
        reloj.fijarEnLima(fechaHoraLima);
    }


    private static final AtomicInteger SECUENCIA_DNI = new AtomicInteger(20_000_000);

    /** Empresa con su área, cargo y usuario de RRHH listos para usar. */
    protected record Escenario(Empresa empresa, Usuario rrhh, UUID areaId, UUID cargoId) {
    }

    protected Escenario nuevoEscenario() throws Exception {
        return nuevoEscenario(nuevaEmpresa());
    }

    protected Escenario nuevoEscenario(Empresa empresa) throws Exception {
        Usuario rrhh = nuevoUsuario(empresa, Rol.RRHH);
        UUID areaId = crearId(rrhh, "/api/v1/areas", Map.of("nombre", "Producción"));
        UUID cargoId = crearId(rrhh, "/api/v1/cargos",
                Map.of("areaId", areaId.toString(), "nombre", "Operario", "sueldoReferencial", "1500.00"));
        return new Escenario(empresa, rrhh, areaId, cargoId);
    }

    protected static String dniNuevo() {
        return String.valueOf(SECUENCIA_DNI.incrementAndGet());
    }

    protected UUID crearId(Usuario como, String ruta, Map<String, ?> cuerpo) throws Exception {
        String respuesta = mvc.perform(post(ruta).header("Authorization", bearer(como))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(respuesta).get("id").asString());
    }

    protected Map<String, Object> cuerpoEmpleado(Escenario e, String nombres, String apellidos, String ingreso) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("dni", dniNuevo());
        cuerpo.put("nombres", nombres);
        cuerpo.put("apellidos", apellidos);
        cuerpo.put("fechaNacimiento", "1990-05-10");
        cuerpo.put("email", nombres.toLowerCase().replace(" ", ".") + "@ejemplo.pe");
        cuerpo.put("fechaIngreso", ingreso);
        cuerpo.put("areaId", e.areaId().toString());
        cuerpo.put("cargoId", e.cargoId().toString());
        return cuerpo;
    }

    protected UUID crearEmpleado(Escenario e, String nombres, String apellidos, String ingreso) throws Exception {
        return crearId(e.rrhh(), "/api/v1/empleados", cuerpoEmpleado(e, nombres, apellidos, ingreso));
    }

    protected UUID crearEmpleadoConJefe(Escenario e, String nombres, String ingreso, UUID jefeId) throws Exception {
        Map<String, Object> cuerpo = cuerpoEmpleado(e, nombres, "Prueba", ingreso);
        cuerpo.put("jefeId", jefeId.toString());
        return crearId(e.rrhh(), "/api/v1/empleados", cuerpo);
    }

    /** Usuario con rol EMPLEADO vinculado a un empleado (portal del empleado). */
    protected Usuario usuarioDeEmpleado(Escenario e, UUID empleadoId) {
        String email = "empleado." + UUID.randomUUID() + "@ejemplo.pe";
        return usuarios.save(new Usuario(e.empresa().getId(), email, "Empleado portal",
                encoder.encode(CLAVE_PRUEBA), Rol.EMPLEADO, empleadoId));
    }

    protected String leer(Usuario como, String ruta) throws Exception {
        return mvc.perform(get(ruta).header("Authorization", bearer(como)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
