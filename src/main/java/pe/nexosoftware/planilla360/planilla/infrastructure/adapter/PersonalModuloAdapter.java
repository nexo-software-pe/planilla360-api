package pe.nexosoftware.planilla360.planilla.infrastructure.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.ContratoResumen;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaEmpresaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaPersonalPort;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoComision;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoRegimenPensionario;

/**
 * Capa anticorrupción: traduce las APIs públicas de {@code empleado} y {@code empresa} a lo que la planilla necesita,
 * sin que el núcleo conozca esos módulos.
 */
@Component
class PersonalModuloAdapter implements ConsultaPersonalPort, ConsultaEmpresaPort {

    private final EmpleadoApi empleados;
    private final EmpresaApi empresas;

    PersonalModuloAdapter(EmpleadoApi empleados, EmpresaApi empresas) {
        this.empleados = empleados;
        this.empresas = empresas;
    }

    @Override
    public List<Trabajador> vigentesEntre(LocalDate desde, LocalDate hasta) {
        return empleados.vigentesEntre(desde, hasta).stream()
                .map(e -> new Trabajador(e.id(), e.nombreCompleto(), e.fechaIngreso(), e.fechaCese()))
                .toList();
    }

    @Override
    public Optional<ContratoVigente> contratoVigenteEn(UUID empleadoId, LocalDate fecha) {
        return empleados.contratoVigenteEn(empleadoId, fecha).map(PersonalModuloAdapter::aContrato);
    }

    @Override
    public List<Hijo> hijosDe(UUID empleadoId) {
        return empleados.hijosDe(empleadoId).stream()
                .map(h -> new Hijo(h.fechaNacimiento(), h.estudia()))
                .toList();
    }

    @Override
    public RegimenLaboral regimenLaboral() {
        EmpresaResumen empresa = empresas.buscarPorId(TenantContext.requerido()).orElseThrow();
        return switch (empresa.regimenLaboral()) {
            case GENERAL -> RegimenLaboral.GENERAL;
            case PEQUENA_EMPRESA -> RegimenLaboral.PEQUENA_EMPRESA;
        };
    }

    private static ContratoVigente aContrato(ContratoResumen c) {
        return new ContratoVigente(
                c.empleadoId(),
                Dinero.de(c.sueldoBasico()),
                c.jornadaSemanal(),
                TipoRegimenPensionario.valueOf(c.regimenPensionario()),
                c.afpId(),
                c.tipoComision() == null ? null : TipoComision.valueOf(c.tipoComision()));
    }
}
