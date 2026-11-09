package pe.nexosoftware.planilla360.planilla.infrastructure.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.planilla.application.port.in.PrecalcularPlanillaUseCase;

/**
 * Cada noche a las 23:00 (hora de Lima) revisa si es el último día hábil del mes y, de serlo, deja en borrador la
 * planilla mensual de todas las empresas activas. También deja en el log el recordatorio de gratificación o CTS. Con
 * varias instancias de la API, ShedLock garantiza que solo una ejecute el proceso a la vez.
 */
@Component
public class PrecalculoPlanillaJob {

    public static final String NOMBRE_BLOQUEO = "planilla-precalculo-mensual";
    private static final Logger LOG = LoggerFactory.getLogger(PrecalculoPlanillaJob.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Duration BLOQUEO_MAXIMO = Duration.ofMinutes(30);
    private static final Duration BLOQUEO_MINIMO = Duration.ofMinutes(5);

    private final EmpresaApi empresas;
    private final PrecalcularPlanillaUseCase precalculo;
    private final LockingTaskExecutor bloqueo;
    private final Clock reloj;

    public PrecalculoPlanillaJob(
            EmpresaApi empresas, PrecalcularPlanillaUseCase precalculo, LockingTaskExecutor bloqueo, Clock reloj) {
        this.empresas = empresas;
        this.precalculo = precalculo;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${app.jobs.planilla.cron:0 0 23 * * *}", zone = "America/Lima")
    public void programado() {
        ejecutar(reloj.instant().atZone(LIMA).toLocalDate());
    }

    /**
     * Pre-calcula la planilla de todas las empresas si {@code hoy} es el último día hábil, a menos que otra instancia
     * tenga el bloqueo.
     *
     * @return {@code true} si esta instancia ejecutó el proceso; {@code false} si otra lo tenía
     */
    public boolean ejecutar(LocalDate hoy) {
        boolean[] ejecutado = {false};
        bloqueo.executeWithLock(
                (Runnable) () -> {
                    ejecutado[0] = true;
                    recorrerEmpresas(hoy);
                },
                new LockConfiguration(reloj.instant(), NOMBRE_BLOQUEO, BLOQUEO_MAXIMO, BLOQUEO_MINIMO));
        return ejecutado[0];
    }

    private void recorrerEmpresas(LocalDate hoy) {
        AtomicInteger generadas = new AtomicInteger();
        empresas.porCadaEmpresaActiva(empresaId -> {
            precalculo.precalcularSiCorresponde(hoy).ifPresent(id -> generadas.incrementAndGet());
            precalculo.recordatorioDeBeneficio(hoy)
                    .filter(aviso -> hoy.getDayOfMonth() == 1)
                    .ifPresent(aviso -> LOG.info("Recordatorio para la empresa {}: {}", empresaId, aviso));
        });
        LOG.info("Pre-cálculo de planillas del {}: {} planillas en borrador generadas", hoy, generadas.get());
    }
}
