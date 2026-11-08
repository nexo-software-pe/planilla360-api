package pe.nexosoftware.planilla360.asistencia.infrastructure.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.in.ConsolidarAsistenciaUseCase;
import pe.nexosoftware.planilla360.asistencia.domain.service.ConsolidadorAsistencia;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;

/**
 * Consolida cada madrugada la asistencia del día anterior de todas las empresas activas. Con varias
 * instancias de la API corriendo, ShedLock garantiza que solo una ejecute el proceso a la vez.
 */
@Component
public class ConsolidacionDiariaJob {

    public static final String NOMBRE_BLOQUEO = "asistencia-consolidacion-diaria";

    private static final Logger LOG = LoggerFactory.getLogger(ConsolidacionDiariaJob.class);
    private static final Duration BLOQUEO_MAXIMO = Duration.ofMinutes(15);
    private static final Duration BLOQUEO_MINIMO = Duration.ofMinutes(1);

    private final EmpresaApi empresas;
    private final ConsolidarAsistenciaUseCase consolidacion;
    private final LockingTaskExecutor bloqueo;
    private final Clock reloj;

    public ConsolidacionDiariaJob(
            EmpresaApi empresas, ConsolidarAsistenciaUseCase consolidacion, LockingTaskExecutor bloqueo, Clock reloj) {
        this.empresas = empresas;
        this.consolidacion = consolidacion;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${app.jobs.asistencia.cron:0 30 0 * * *}", zone = "America/Lima")
    public void programado() {
        ejecutar(reloj.instant().atZone(ConsolidadorAsistencia.ZONA_LIMA).toLocalDate().minusDays(1));
    }

    /**
     * Consolida la fecha para todas las empresas, si nadie más tiene el bloqueo.
     *
     * @return {@code true} si esta instancia ejecutó el proceso; {@code false} si otra lo tenía
     */
    public boolean ejecutar(LocalDate fecha) {
        boolean[] ejecutado = {false};
        bloqueo.executeWithLock(
                (Runnable) () -> {
                    ejecutado[0] = true;
                    consolidarTodas(fecha);
                },
                new LockConfiguration(reloj.instant(), NOMBRE_BLOQUEO, BLOQUEO_MAXIMO, BLOQUEO_MINIMO));
        return ejecutado[0];
    }

    private void consolidarTodas(LocalDate fecha) {
        int total = 0;
        for (UUID empresaId : empresas.idsActivas()) {
            TenantContext.establecer(empresaId);
            try {
                total += consolidacion.consolidar(fecha);
            } catch (RuntimeException e) {
                LOG.error("Falló la consolidación de asistencia de la empresa {} para {}", empresaId, fecha, e);
            } finally {
                TenantContext.limpiar();
            }
        }
        LOG.info("Consolidación de asistencia del {} terminada: {} empleados procesados", fecha, total);
    }
}
