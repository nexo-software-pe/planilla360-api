package pe.nexosoftware.planilla360.vacaciones.infrastructure.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.EmpleadoResumen;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.RegimenEmpresaPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PoliticaVacaciones;

/**
 * Capa anticorrupción: traduce las APIs públicas de {@code empleado} y {@code empresa} a lo que
 * vacaciones necesita, sin que el núcleo conozca esos módulos.
 */
@Component
class EmpleadoEmpresaModuloAdapter implements ConsultaEmpleadoPort, RegimenEmpresaPort {

    private final EmpleadoApi empleados;
    private final EmpresaApi empresas;

    EmpleadoEmpresaModuloAdapter(EmpleadoApi empleados, EmpresaApi empresas) {
        this.empleados = empleados;
        this.empresas = empresas;
    }

    @Override
    public Optional<EmpleadoInfo> deUsuario(UUID usuarioId) {
        return empleados.deUsuario(usuarioId).map(EmpleadoEmpresaModuloAdapter::aInfo);
    }

    @Override
    public Optional<EmpleadoInfo> buscar(UUID empleadoId) {
        return empleados.buscar(empleadoId).map(EmpleadoEmpresaModuloAdapter::aInfo);
    }

    @Override
    public List<EmpleadoInfo> vigentesEn(LocalDate fecha) {
        return empleados.vigentesEn(fecha).stream().map(EmpleadoEmpresaModuloAdapter::aInfo).toList();
    }

    @Override
    public PoliticaVacaciones politicaDeLaEmpresa() {
        RegimenLaboral regimen = empresas.buscarPorId(TenantContext.requerido())
                .orElseThrow(() -> new IllegalStateException("La empresa del contexto no existe."))
                .regimenLaboral();
        return switch (regimen) {
            case GENERAL -> PoliticaVacaciones.general();
            case PEQUENA_EMPRESA -> PoliticaVacaciones.pequenaEmpresa();
        };
    }

    private static EmpleadoInfo aInfo(EmpleadoResumen r) {
        return new EmpleadoInfo(r.id(), r.nombreCompleto(), r.jefeId(), r.fechaIngreso(), r.fechaCese());
    }
}
