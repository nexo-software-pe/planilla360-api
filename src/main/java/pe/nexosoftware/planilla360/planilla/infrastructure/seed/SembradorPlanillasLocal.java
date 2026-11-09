package pe.nexosoftware.planilla360.planilla.infrastructure.seed;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.planilla.application.model.FiltroPlanillas;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.in.GestionarPlanillasUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

/**
 * Solo perfil {@code local}: deja a cada empresa con seis meses de planillas cerradas (con la gratificación de julio y
 * la CTS de mayo cuando caen en la ventana) para poder probar reportes, boletas y anulaciones sin empezar de cero.
 * Usa los casos de uso reales, así que los montos son los que calcularía el sistema. Es idempotente: si la empresa ya
 * tiene planillas no hace nada. No publica eventos: son datos históricos, no cierres nuevos.
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "app.seed.planillas", havingValue = "true", matchIfMissing = true)
class SembradorPlanillasLocal implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SembradorPlanillasLocal.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final int MESES_DE_HISTORIA = 6;

    private final EmpresaApi empresas;
    private final GestionarPlanillasUseCase planillas;
    private final PlanillaPort repositorio;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    SembradorPlanillasLocal(
            EmpresaApi empresas,
            GestionarPlanillasUseCase planillas,
            PlanillaPort repositorio,
            TransactionTemplate transaccion,
            Clock reloj) {
        this.empresas = empresas;
        this.planillas = planillas;
        this.repositorio = repositorio;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        empresas.porCadaEmpresaActiva(empresaId -> sembrar(empresaId));
    }

    private void sembrar(UUID empresaId) {
        Pagina<ResumenPlanilla> existentes = planillas.listar(new FiltroPlanillas(null, null, null), 0, 1);
        if (existentes.totalElementos() > 0) {
            return;
        }
        int cerradas = 0;
        for (Periodo periodo : mesesDeHistoria()) {
            for (TipoPlanilla tipo : tiposDe(periodo)) {
                try {
                    cerrarDirecto(planillas.generar(periodo, tipo).id(), empresaId + "-" + tipo + "-" + periodo);
                    cerradas++;
                } catch (RuntimeException e) {
                    LOG.warn("Seed de planillas: no se pudo generar {} {} de la empresa {}: {}", tipo, periodo,
                            empresaId, e.getMessage());
                }
            }
        }
        LOG.info("Seed de planillas: empresa {} con {} planillas cerradas", empresaId, cerradas);
    }

    private List<Periodo> mesesDeHistoria() {
        YearMonth actual = YearMonth.from(reloj.instant().atZone(LIMA));
        List<Periodo> meses = new ArrayList<>();
        for (int atras = MESES_DE_HISTORIA; atras >= 1; atras--) {
            meses.add(new Periodo(actual.minusMonths(atras)));
        }
        return meses;
    }

    private static List<TipoPlanilla> tiposDe(Periodo periodo) {
        List<TipoPlanilla> tipos = new ArrayList<>();
        tipos.add(TipoPlanilla.MENSUAL);
        if (periodo.mes() == 7 || periodo.mes() == 12) {
            tipos.add(TipoPlanilla.GRATIFICACION);
        }
        if (periodo.mes() == 5 || periodo.mes() == 11) {
            tipos.add(TipoPlanilla.CTS);
        }
        return tipos;
    }

    /** Cierra sin pasar por las validaciones: la asistencia del seed solo cubre las últimas dos semanas. */
    private void cerrarDirecto(UUID planillaId, String clave) {
        transaccion.executeWithoutResult(estado -> {
            Planilla planilla = repositorio.buscarParaActualizar(planillaId).orElseThrow();
            planilla.cerrar(clave, new ResultadoValidacion(List.of()), Instant.now(reloj));
            repositorio.guardar(planilla);
        });
    }
}
