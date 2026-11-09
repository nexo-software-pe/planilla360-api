package pe.nexosoftware.planilla360.boleta.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.boleta.BoletaGeneradaEvento;
import pe.nexosoftware.planilla360.boleta.almacenamiento.AlmacenArchivos;
import pe.nexosoftware.planilla360.boleta.entity.Boleta;
import pe.nexosoftware.planilla360.boleta.model.EstadoBoleta;
import pe.nexosoftware.planilla360.boleta.pdf.DatosBoletaPdf;
import pe.nexosoftware.planilla360.boleta.pdf.DatosBoletaPdf.Renglon;
import pe.nexosoftware.planilla360.boleta.pdf.GeneradorBoletaPdf;
import pe.nexosoftware.planilla360.boleta.repository.BoletaRepository;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.FichaEmpleado;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.planilla.PlanillaApi;
import pe.nexosoftware.planilla360.planilla.PlanillaApi.ConceptoBoleta;
import pe.nexosoftware.planilla360.planilla.PlanillaApi.DatosParaBoletas;
import pe.nexosoftware.planilla360.planilla.PlanillaApi.LineaBoleta;

/**
 * Genera la boleta de un trabajador: arma los datos, produce el PDF, lo guarda y avisa con un evento. Es idempotente:
 * si la boleta ya tiene su PDF no hace nada, así un mensaje repetido por la cola no la duplica.
 */
@Service
public class GeneracionBoletaService {

    static final String TIPO_PDF = "application/pdf";

    private final BoletaRepository boletas;
    private final PlanillaApi planillas;
    private final EmpleadoApi empleados;
    private final EmpresaApi empresas;
    private final GeneradorBoletaPdf generador;
    private final AlmacenArchivos almacen;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public GeneracionBoletaService(
            BoletaRepository boletas,
            PlanillaApi planillas,
            EmpleadoApi empleados,
            EmpresaApi empresas,
            GeneradorBoletaPdf generador,
            AlmacenArchivos almacen,
            ApplicationEventPublisher eventos,
            Clock reloj) {
        this.boletas = boletas;
        this.planillas = planillas;
        this.empleados = empleados;
        this.empresas = empresas;
        this.generador = generador;
        this.almacen = almacen;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * Transacción propia: sin mensajería este método se invoca desde un oyente posterior a la confirmación del cierre,
     * donde unirse a la transacción ya terminada dejaría los cambios sin confirmar.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generar(UUID planillaId, UUID empleadoId) {
        Boleta boleta = boletas.findByPlanillaIdAndEmpleadoId(planillaId, empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta solicitada."));
        if (boleta.tienePdf()) {
            return;
        }
        boleta.registrarIntento();
        UUID empresaId = TenantContext.requerido();
        DatosParaBoletas planilla = planillas.datosParaBoletas(planillaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la planilla de la boleta."));
        LineaBoleta linea = planilla.lineas().stream()
                .filter(l -> l.empleadoId().equals(empleadoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El trabajador no figura en la planilla."));
        FichaEmpleado ficha = empleados.fichaDe(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el trabajador de la boleta."));
        EmpresaResumen empresa = empresas.buscarPorId(empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa de la boleta."));

        byte[] pdf = generador.generar(armarDatos(planilla, linea, ficha, empresa));
        String clave = "%s/%s/%s/%s.pdf".formatted(empresaId, planilla.periodo(), planillaId, empleadoId);
        almacen.guardar(clave, pdf, TIPO_PDF);
        boleta.registrarGeneracion(clave, sha256(pdf), pdf.length, Instant.now(reloj));
        eventos.publishEvent(new BoletaGeneradaEvento(
                boleta.getId(), empresaId, planillaId, empleadoId, planilla.periodo().toString(), planilla.tipo()));
    }

    /** En transacción propia: se invoca después de revertirse el intento, cuando ya se agotaron los reintentos. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallida(UUID planillaId, UUID empleadoId, String motivo) {
        boletas.findByPlanillaIdAndEmpleadoId(planillaId, empleadoId).ifPresent(b -> {
            if (b.getEstado() == EstadoBoleta.PENDIENTE) {
                b.marcarFallida(motivo);
            }
        });
    }

    private static DatosBoletaPdf armarDatos(
            DatosParaBoletas planilla, LineaBoleta linea, FichaEmpleado ficha, EmpresaResumen empresa) {
        return new DatosBoletaPdf(
                empresa.razonSocial(),
                empresa.ruc(),
                titulo(planilla.tipo()),
                planilla.periodo().toString(),
                ficha.nombreCompleto(),
                ficha.dni(),
                ficha.cargo(),
                ficha.area(),
                ficha.fechaIngreso(),
                linea.regimenPensionario(),
                renglones(linea.conceptos(), "INGRESO"),
                renglones(linea.conceptos(), "DESCUENTO"),
                renglones(linea.conceptos(), "APORTE_EMPLEADOR"),
                linea.totalIngresos(),
                linea.totalDescuentos(),
                linea.neto());
    }

    private static List<Renglon> renglones(List<ConceptoBoleta> conceptos, String tipo) {
        return conceptos.stream()
                .filter(c -> c.tipo().equals(tipo))
                .map(c -> new Renglon(c.descripcion(), c.monto()))
                .toList();
    }

    private static String titulo(String tipo) {
        return switch (tipo) {
            case "GRATIFICACION" -> "Boleta de gratificación";
            case "CTS" -> "Constancia de depósito de CTS";
            default -> "Boleta de pago de remuneraciones";
        };
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
