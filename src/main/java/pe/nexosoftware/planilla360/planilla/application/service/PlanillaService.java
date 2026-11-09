package pe.nexosoftware.planilla360.planilla.application.service;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.planilla.application.model.FiltroPlanillas;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.in.GestionarPlanillasUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.MedidorCalculoPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.SemestreDeBeneficio;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

@Service
@Transactional(readOnly = true)
public class PlanillaService implements GestionarPlanillasUseCase {

    static final String ENTIDAD = "PLANILLA";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final PlanillaPort planillas;
    private final ArmadorDeLineas armador;
    private final ValidadorDePlanilla validador;
    private final PublicadorEventosPort eventos;
    private final Clock reloj;
    private final MedidorCalculoPort medidor;

    public PlanillaService(
            PlanillaPort planillas,
            ArmadorDeLineas armador,
            ValidadorDePlanilla validador,
            PublicadorEventosPort eventos,
            Clock reloj,
            MedidorCalculoPort medidor) {
        this.planillas = planillas;
        this.armador = armador;
        this.validador = validador;
        this.eventos = eventos;
        this.reloj = reloj;
        this.medidor = medidor;
    }

    @Override
    @Transactional
    public Planilla crear(Periodo periodo, TipoPlanilla tipo) {
        exigirPeriodoValido(periodo, tipo);
        if (planillas.existeVigente(periodo, tipo)) {
            throw new ConflictoException(
                    "Ya existe una planilla " + tipo + " vigente del periodo " + periodo + ". Anúlela para rehacerla.");
        }
        Planilla borrador = Planilla.crearBorrador(UUID.randomUUID(), TenantContext.requerido(), periodo, tipo);
        Planilla guardada = planillas.guardar(borrador);
        eventos.registrarCambio(guardada.id(), Accion.CREAR, null, ResumenPlanilla.de(guardada));
        return guardada;
    }

    @Override
    @Transactional
    public Planilla calcular(UUID planillaId) {
        Planilla planilla = planillas.buscarParaActualizar(planillaId).orElseThrow(PlanillaService::noExiste);
        ResumenPlanilla antes = ResumenPlanilla.de(planilla);
        planilla.estado().alCalcular();
        planilla.registrarCalculo(
                medidor.medir(planilla.tipo(), () -> armador.armar(planilla.periodo(), planilla.tipo())));
        Planilla guardada = planillas.guardar(planilla);
        eventos.registrarCambio(guardada.id(), Accion.ACTUALIZAR, antes, ResumenPlanilla.de(guardada));
        return guardada;
    }

    @Override
    public Planilla simular(Periodo periodo, TipoPlanilla tipo) {
        exigirPeriodoValido(periodo, tipo);
        Planilla simulada = Planilla.crearBorrador(UUID.randomUUID(), TenantContext.requerido(), periodo, tipo);
        simulada.registrarCalculo(medidor.medir(tipo, () -> armador.armar(periodo, tipo)));
        return simulada;
    }

    @Override
    @Transactional
    public Planilla generar(Periodo periodo, TipoPlanilla tipo) {
        Planilla creada = crear(periodo, tipo);
        return calcular(creada.id());
    }

    @Override
    public Planilla obtener(UUID planillaId) {
        return planillas.buscar(planillaId).orElseThrow(PlanillaService::noExiste);
    }

    @Override
    public Pagina<ResumenPlanilla> listar(FiltroPlanillas filtro, int pagina, int tamano) {
        return planillas.listar(filtro, pagina, tamano);
    }

    @Override
    public ResultadoValidacion validar(UUID planillaId) {
        return validador.validar(obtener(planillaId));
    }

    private void exigirPeriodoValido(Periodo periodo, TipoPlanilla tipo) {
        if (periodo.compareTo(Periodo.de(reloj.instant().atZone(LIMA).toLocalDate())) > 0) {
            throw new ReglaDeNegocioException("No se puede trabajar la planilla de un periodo futuro: " + periodo + ".");
        }
        switch (tipo) {
            case MENSUAL -> {
                // Cualquier mes tiene planilla mensual.
            }
            case GRATIFICACION -> SemestreDeBeneficio.deGratificacion(periodo);
            case CTS -> SemestreDeBeneficio.deCts(periodo);
        }
    }

    static RecursoNoEncontradoException noExiste() {
        return new RecursoNoEncontradoException("No existe la planilla indicada.");
    }
}
