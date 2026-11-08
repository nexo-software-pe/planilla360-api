package pe.nexosoftware.planilla360.asistencia.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.asistencia.application.port.in.ConsolidarAsistenciaUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.in.ConsultarAsistenciaUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.in.ConsultarAsistenciaUseCase.ResumenMensual;
import pe.nexosoftware.planilla360.asistencia.application.port.in.RegistrarMarcacionUseCase;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.AsistenciaDiariaResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.ConsolidacionRequest;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.ConsolidacionResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.MarcacionResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.mapper.AsistenciaWebMapper;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/asistencia")
@Tag(name = "Asistencia", description = "Marcaciones, asistencia consolidada y resumen mensual")
public class AsistenciaController {

    private static final String PERSONAL = "hasAnyRole('RRHH', 'ADMIN_EMPRESA')";
    private static final String CUALQUIER_TRABAJADOR = "hasAnyRole('EMPLEADO', 'RRHH', 'ADMIN_EMPRESA')";

    private final RegistrarMarcacionUseCase marcaciones;
    private final ConsultarAsistenciaUseCase consultas;
    private final ConsolidarAsistenciaUseCase consolidacion;
    private final AsistenciaWebMapper mapper;

    public AsistenciaController(
            RegistrarMarcacionUseCase marcaciones,
            ConsultarAsistenciaUseCase consultas,
            ConsolidarAsistenciaUseCase consolidacion,
            AsistenciaWebMapper mapper) {
        this.marcaciones = marcaciones;
        this.consultas = consultas;
        this.consolidacion = consolidacion;
        this.mapper = mapper;
    }

    /** Marca la entrada o la salida de quien llama (el sistema decide cuál corresponde). */
    @PostMapping("/marcaciones")
    @PreAuthorize(CUALQUIER_TRABAJADOR)
    @ResponseStatus(HttpStatus.CREATED)
    public MarcacionResponse marcar() {
        return mapper.aResponse(marcaciones.registrar(UsuarioAutenticado.requerido().usuarioId()));
    }

    @GetMapping("/mia")
    @PreAuthorize(CUALQUIER_TRABAJADOR)
    public List<AsistenciaDiariaResponse> mia(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return mapper.aResponses(consultas.deMiMes(UsuarioAutenticado.requerido().usuarioId(), mes));
    }

    @GetMapping
    @PreAuthorize(PERSONAL)
    public List<AsistenciaDiariaResponse> deEmpleado(
            @RequestParam UUID empleadoId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return mapper.aResponses(consultas.deEmpleado(empleadoId, mes));
    }

    @GetMapping("/resumen")
    @PreAuthorize(PERSONAL)
    public List<ResumenMensual> resumen(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return consultas.resumen(mes);
    }

    /** Vuelve a consolidar un día ya terminado (la consolidación automática corre cada madrugada). */
    @PostMapping("/consolidaciones")
    @PreAuthorize(PERSONAL)
    public ConsolidacionResponse consolidar(@Valid @RequestBody ConsolidacionRequest request) {
        return new ConsolidacionResponse(request.fecha(), consolidacion.consolidar(request.fecha()));
    }
}
