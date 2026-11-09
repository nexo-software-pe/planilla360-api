package pe.nexosoftware.planilla360.boleta.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pe.nexosoftware.planilla360.boleta.BoletaApi;
import pe.nexosoftware.planilla360.boleta.almacenamiento.AlmacenArchivos;
import pe.nexosoftware.planilla360.boleta.dto.BoletaResponse;
import pe.nexosoftware.planilla360.boleta.dto.EstadoGeneracionResponse;
import pe.nexosoftware.planilla360.boleta.entity.Boleta;
import pe.nexosoftware.planilla360.boleta.mensajeria.ColaBoletas;
import pe.nexosoftware.planilla360.boleta.mensajeria.TrabajoBoleta;
import pe.nexosoftware.planilla360.boleta.model.EstadoBoleta;
import pe.nexosoftware.planilla360.boleta.repository.BoletaRepository;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.EmpleadoResumen;
import pe.nexosoftware.planilla360.planilla.PlanillaApi;
import pe.nexosoftware.planilla360.planilla.PlanillaApi.DatosParaBoletas;
import pe.nexosoftware.planilla360.planilla.PlanillaApi.LineaBoleta;

/** Generación masiva (una boleta por trabajador, encolada), consulta de estado y descarga. */
@Service
public class BoletaService implements BoletaApi {

    private final BoletaRepository boletas;
    private final PlanillaApi planillas;
    private final EmpleadoApi empleados;
    private final ColaBoletas cola;
    private final AlmacenArchivos almacen;
    private final TransactionTemplate transaccionNueva;
    private final Clock reloj;

    public BoletaService(
            BoletaRepository boletas,
            PlanillaApi planillas,
            EmpleadoApi empleados,
            ColaBoletas cola,
            AlmacenArchivos almacen,
            PlatformTransactionManager transacciones,
            Clock reloj) {
        this.boletas = boletas;
        this.planillas = planillas;
        this.empleados = empleados;
        this.cola = cola;
        this.almacen = almacen;
        this.transaccionNueva = new TransactionTemplate(transacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.reloj = reloj;
    }

    /**
     * Crea las boletas que falten, reencola las que fallaron y encola la generación de las pendientes. Se puede
     * repetir sin riesgo: lo ya generado no se toca. La planilla debe estar cerrada.
     */
    public EstadoGeneracionResponse generarMasivo(UUID planillaId) {
        UUID empresaId = TenantContext.requerido();
        List<TrabajoBoleta> trabajos = transaccionNueva.execute(estado -> {
            DatosParaBoletas planilla = planillas.datosParaBoletas(planillaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la planilla indicada."));
            if (!"CERRADA".equals(planilla.estado())) {
                throw new ReglaDeNegocioException("Solo se generan boletas de una planilla cerrada.");
            }
            Map<UUID, Boleta> existentes = new HashMap<>();
            boletas.findByPlanillaId(planillaId).forEach(b -> existentes.put(b.getEmpleadoId(), b));
            List<TrabajoBoleta> pendientes = new ArrayList<>();
            for (LineaBoleta linea : planilla.lineas()) {
                Boleta boleta = existentes.get(linea.empleadoId());
                if (boleta == null) {
                    boleta = boletas.save(new Boleta(
                            planillaId, linea.empleadoId(), planilla.periodo().toString(), planilla.tipo()));
                } else if (boleta.getEstado() == EstadoBoleta.FALLIDA) {
                    boleta.reencolar();
                }
                if (boleta.getEstado() == EstadoBoleta.PENDIENTE) {
                    pendientes.add(new TrabajoBoleta(empresaId, planillaId, linea.empleadoId()));
                }
            }
            return pendientes;
        });
        // Fuera de la transacción: el consumidor debe encontrar las filas ya confirmadas.
        trabajos.forEach(cola::encolar);
        return estado(planillaId);
    }

    @Transactional(readOnly = true)
    public EstadoGeneracionResponse estado(UUID planillaId) {
        Map<EstadoBoleta, Long> conteo = new EnumMap<>(EstadoBoleta.class);
        for (Object[] fila : boletas.contarPorEstado(planillaId)) {
            conteo.put((EstadoBoleta) fila[0], (Long) fila[1]);
        }
        long pendientes = conteo.getOrDefault(EstadoBoleta.PENDIENTE, 0L);
        long generadas = conteo.getOrDefault(EstadoBoleta.GENERADA, 0L);
        long enviadas = conteo.getOrDefault(EstadoBoleta.ENVIADA, 0L);
        long fallidas = conteo.getOrDefault(EstadoBoleta.FALLIDA, 0L);
        long total = pendientes + generadas + enviadas + fallidas;
        String resumen;
        if (total == 0) {
            resumen = "SIN_INICIAR";
        } else if (pendientes > 0) {
            resumen = "EN_PROCESO";
        } else if (fallidas > 0) {
            resumen = "CON_FALLOS";
        } else {
            resumen = "COMPLETADA";
        }
        return new EstadoGeneracionResponse(planillaId, resumen, total, pendientes, generadas, enviadas, fallidas);
    }

    @Transactional(readOnly = true)
    public Pagina<BoletaResponse> deLaPlanilla(UUID planillaId, Pageable pageable) {
        return Pagina.de(boletas.findByPlanillaId(planillaId, pageable), BoletaService::aRespuesta);
    }

    @Transactional(readOnly = true)
    public Pagina<BoletaResponse> mias(Pageable pageable) {
        EmpleadoResumen empleado = empleados.deUsuario(UsuarioAutenticado.requerido().usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tu cuenta no está vinculada a un empleado."));
        return Pagina.de(
                boletas.findByEmpleadoIdOrderByCreatedAtDesc(empleado.id(), pageable), BoletaService::aRespuesta);
    }

    /** PDF de la boleta. Un trabajador solo accede a las suyas; las ajenas se responden como inexistentes. */
    @Transactional(readOnly = true)
    public byte[] pdf(UUID boletaId) {
        Boleta boleta = boletas.findById(boletaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta solicitada."));
        UsuarioAutenticado usuario = UsuarioAutenticado.requerido();
        if (usuario.rol() == Rol.EMPLEADO) {
            boolean esSuya = empleados.deUsuario(usuario.usuarioId())
                    .map(e -> e.id().equals(boleta.getEmpleadoId()))
                    .orElse(false);
            if (!esSuya) {
                throw new RecursoNoEncontradoException("No existe la boleta solicitada.");
            }
        }
        if (!boleta.tienePdf()) {
            throw new ReglaDeNegocioException("La boleta aún no está disponible (estado " + boleta.getEstado() + ").");
        }
        return almacen.leer(boleta.getArchivoClave())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el archivo de la boleta."));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarEnviada(UUID boletaId) {
        boletas.findById(boletaId).ifPresent(b -> {
            if (b.getEstado() == EstadoBoleta.GENERADA || b.getEstado() == EstadoBoleta.ENVIADA) {
                b.registrarEnvio(Instant.now(reloj));
            }
        });
    }

    private static BoletaResponse aRespuesta(Boleta b) {
        return new BoletaResponse(b.getId(), b.getPlanillaId(), b.getEmpleadoId(), b.getPeriodo(), b.getTipo(),
                b.getEstado(), b.getGeneradaEn(), b.getEnviadaEn());
    }
}
