package pe.nexosoftware.planilla360.vacaciones.infrastructure.scheduling;

import java.time.Clock;
import java.time.Duration;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.ActualizarEstadosVacacionesUseCase;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.ActualizarEstadosVacacionesUseCase.Resultado;

/**
 * Cada madrugada pone en goce las vacaciones que empiezan y finaliza las que terminaron, para todas las
 * empresas activas. ShedLock evita que dos instancias lo ejecuten a la vez.
 */
@Component
public class EstadosVacacionesJob {

    public static final String NOMBRE_BLOQUEO = "vacaciones-actualizacion-estados";

    private static final Logger LOG = LoggerFactory.getLogger(EstadosVacacionesJob.class);

    private final EmpresaApi empresas;
    private final ActualizarEstadosVacacionesUseCase estados;
    private final LockingTaskExecutor bloqueo;
    private final Clock reloj;

    public EstadosVacacionesJob(
            EmpresaApi empresas, ActualizarEstadosVacacionesUseCase estados, LockingTaskExecutor bloqueo, Clock reloj) {
        this.empresas = empresas;
        this.estados = estados;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${app.jobs.vacaciones.cron:0 10 0 * * *}", zone = "America/Lima")
    public void programado() {
        ejecutar();
    }

    /** @return {@code true} si esta instancia ejecutó el proceso; {@code false} si otra tenía el bloqueo */
    public boolean ejecutar() {
        boolean[] ejecutado = {false};
        bloqueo.executeWithLock(
                (Runnable) () -> {
                    ejecutado[0] = true;
                    empresas.porCadaEmpresaActiva(empresaId -> {
                        Resultado r = estados.actualizar();
                        LOG.info("Vacaciones de la empresa {}: {} iniciadas, {} finalizadas", empresaId,
                                r.iniciadas(), r.finalizadas());
                    });
                },
                new LockConfiguration(
                        reloj.instant(), NOMBRE_BLOQUEO, Duration.ofMinutes(15), Duration.ofMinutes(1)));
        return ejecutado[0];
    }
}
