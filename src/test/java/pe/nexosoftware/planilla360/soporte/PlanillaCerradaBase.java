package pe.nexosoftware.planilla360.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;

/**
 * Prepara una empresa con tres trabajadores, su horario y la asistencia de marzo de 2030 consolidada, lista para
 * calcular y cerrar la planilla mensual. Las pruebas de boletas, correos y reportes parten de aquí.
 */
public abstract class PlanillaCerradaBase extends PersonalBase {

    protected static final String PLANILLAS = "/api/v1/planillas";

    protected Escenario e;
    protected Usuario admin;
    protected UUID ana;
    protected UUID beto;
    protected UUID carla;

    protected void prepararEmpresaConTrabajadores() throws Exception {
        ahoraEs("2030-04-02T10:00");
        e = nuevoEscenario();
        admin = nuevoUsuario(e.empresa(), Rol.ADMIN_EMPRESA);
        UUID afp = UUID.fromString(json.readTree(leer(e.rrhh(), "/api/v1/afps")).get(0).get("id").asString());
        ana = empleadoConContrato("Ana", "2024-01-01", "3000.00", null);
        beto = empleadoConContrato("Beto", "2024-01-01", "5000.00", afp);
        carla = empleadoConContrato("Carla", "2024-01-01", "2000.00", null);
        UUID horario = crearId(e.rrhh(), "/api/v1/horarios", Map.of(
                "nombre", "Oficina", "entrada", "08:00", "salida", "17:00", "toleranciaMin", 10,
                "dias", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")));
        for (UUID empleado : List.of(ana, beto, carla)) {
            enviar(e.rrhh(), "/api/v1/horarios/" + horario + "/asignaciones",
                    Map.of("empleadoId", empleado.toString(), "desde", "2024-01-01")).andExpect(status().isNoContent());
        }
        enviar(e.rrhh(), "/api/v1/asistencia/consolidaciones", Map.of("fecha", "2030-03-31"))
                .andExpect(status().isOk());
    }

    /** Crea, calcula y cierra la planilla mensual de marzo de 2030. Devuelve su identificador. */
    protected UUID cerrarPlanillaDeMarzo() throws Exception {
        String creada = enviar(e.rrhh(), PLANILLAS, Map.of("periodo", "2030-03", "tipo", "MENSUAL"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(creada).get("id").asString());
        mvc.perform(post(PLANILLAS + "/" + id + "/calcular").header("Authorization", bearer(e.rrhh())))
                .andExpect(status().isOk());
        mvc.perform(post(PLANILLAS + "/" + id + "/cerrar").header("Authorization", bearer(e.rrhh()))
                        .header("Idempotency-Key", "cierre-" + id))
                .andExpect(status().isOk());
        return id;
    }

    private UUID empleadoConContrato(String nombre, String ingreso, String sueldo, UUID afp) throws Exception {
        UUID id = crearEmpleado(e, nombre, "Planilla", ingreso);
        Map<String, Object> contrato = new LinkedHashMap<>();
        contrato.put("tipo", "INDEFINIDO");
        contrato.put("fechaInicio", ingreso);
        contrato.put("sueldoBasico", sueldo);
        contrato.put("jornadaSemanal", 48);
        contrato.put("regimenPensionario", afp == null ? "ONP" : "AFP");
        if (afp != null) {
            contrato.put("afpId", afp.toString());
            contrato.put("tipoComision", "FLUJO");
        }
        enviar(e.rrhh(), "/api/v1/empleados/" + id + "/contratos", contrato).andExpect(status().isCreated());
        return id;
    }

    protected ResultActions enviar(Usuario como, String ruta, Object cuerpo) throws Exception {
        return mvc.perform(post(ruta).header("Authorization", bearer(como))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }
}
