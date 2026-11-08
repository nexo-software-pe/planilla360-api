package pe.nexosoftware.planilla360.asistencia.infrastructure.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaFeriadoPort;
import pe.nexosoftware.planilla360.catalogo.CatalogoApi;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.EmpleadoResumen;

/**
 * Capa anticorrupción: traduce las APIs públicas de los módulos {@code empleado} y {@code catalogo} a lo
 * que asistencia necesita, sin que el núcleo conozca esos módulos.
 */
@Component
class EmpleadoModuloAdapter implements ConsultaEmpleadoPort, ConsultaFeriadoPort {

    private final EmpleadoApi empleados;
    private final CatalogoApi catalogo;

    EmpleadoModuloAdapter(EmpleadoApi empleados, CatalogoApi catalogo) {
        this.empleados = empleados;
        this.catalogo = catalogo;
    }

    @Override
    public Optional<EmpleadoInfo> deUsuario(UUID usuarioId) {
        return empleados.deUsuario(usuarioId).map(EmpleadoModuloAdapter::aInfo);
    }

    @Override
    public Optional<EmpleadoInfo> buscar(UUID empleadoId) {
        return empleados.buscar(empleadoId).map(EmpleadoModuloAdapter::aInfo);
    }

    @Override
    public List<EmpleadoInfo> vigentesEn(LocalDate fecha) {
        return empleados.vigentesEn(fecha).stream().map(EmpleadoModuloAdapter::aInfo).toList();
    }

    @Override
    public boolean esFeriado(LocalDate fecha) {
        return catalogo.esFeriado(fecha);
    }

    private static EmpleadoInfo aInfo(EmpleadoResumen resumen) {
        return new EmpleadoInfo(resumen.id(), resumen.nombreCompleto(), resumen.jefeId());
    }
}
