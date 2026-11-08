package pe.nexosoftware.planilla360.planilla.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.event.PlanillaCerrada;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;
import pe.nexosoftware.planilla360.planilla.domain.validation.Incidencia;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

/** Agregado Planilla de una empresa y un periodo: sus líneas solo cambian mientras no esté cerrada. */
public final class Planilla {

    private final UUID id;
    private final UUID empresaId;
    private final Periodo periodo;
    private final TipoPlanilla tipo;
    private EstadoPlanilla estado = EstadoPlanilla.BORRADOR;
    private List<LineaPlanilla> lineas = List.of();
    private String claveIdempotencia;
    private Instant cerradaEn;

    private Planilla(UUID id, UUID empresaId, Periodo periodo, TipoPlanilla tipo) {
        this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
        this.empresaId = Objects.requireNonNull(empresaId, "La empresa es obligatoria");
        this.periodo = Objects.requireNonNull(periodo, "El periodo es obligatorio");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de planilla es obligatorio");
    }

    public static Planilla crearBorrador(UUID id, UUID empresaId, Periodo periodo, TipoPlanilla tipo) {
        return new Planilla(id, empresaId, periodo, tipo);
    }

    /** Reconstruye una planilla ya guardada, con su estado y su cierre (lo usa el adaptador de persistencia). */
    public static Planilla reconstruir(UUID id, UUID empresaId, Periodo periodo, TipoPlanilla tipo,
            EstadoPlanilla estado, List<LineaPlanilla> lineas, String claveIdempotencia, Instant cerradaEn) {
        throw new UnsupportedOperationException("pendiente");
    }

    public UUID id() {
        return id;
    }

    public UUID empresaId() {
        return empresaId;
    }

    public Periodo periodo() {
        return periodo;
    }

    public TipoPlanilla tipo() {
        return tipo;
    }

    public EstadoPlanilla estado() {
        return estado;
    }

    public List<LineaPlanilla> lineas() {
        return lineas;
    }

    /** Guarda el resultado del cálculo (reemplaza al anterior) y pasa la planilla a CALCULADA. */
    public void registrarCalculo(List<LineaPlanilla> nuevasLineas) {
        EstadoPlanilla siguiente = estado.alCalcular();
        if (nuevasLineas.isEmpty()) {
            throw new ReglaDePlanillaException("La planilla no tiene trabajadores para calcular");
        }
        Set<UUID> vistos = new HashSet<>();
        for (LineaPlanilla linea : nuevasLineas) {
            if (!vistos.add(linea.empleadoId())) {
                throw new ReglaDePlanillaException(
                        "El trabajador " + linea.empleadoId() + " tiene más de una línea en la planilla");
            }
        }
        this.lineas = List.copyOf(nuevasLineas);
        this.estado = siguiente;
    }

    public void anular() {
        this.estado = estado.alAnular();
    }

    public Optional<String> claveIdempotencia() {
        return Optional.ofNullable(claveIdempotencia);
    }

    public Optional<Instant> cerradaEn() {
        return Optional.ofNullable(cerradaEn);
    }

    /**
     * Cierra la planilla de forma idempotente: repetir el cierre con la misma clave no hace nada; con otra clave es un
     * conflicto. Solo cierra si la validación previa no tiene incidencias.
     *
     * @return el evento {@link PlanillaCerrada}, o vacío si es la repetición de un cierre ya hecho con la misma clave
     */
    public Optional<PlanillaCerrada> cerrar(String clave, ResultadoValidacion validacion, Instant ahora) {
        Objects.requireNonNull(clave, "La clave de idempotencia es obligatoria");
        Objects.requireNonNull(validacion, "El resultado de la validación es obligatorio");
        Objects.requireNonNull(ahora, "La fecha de cierre es obligatoria");
        if (clave.isBlank()) {
            throw new IllegalArgumentException("La clave de idempotencia no puede estar vacía");
        }
        if (estado == EstadoPlanilla.CERRADA) {
            if (clave.equals(claveIdempotencia)) {
                return Optional.empty();
            }
            throw new ReglaDePlanillaException("La planilla ya está cerrada con otra clave de idempotencia");
        }
        EstadoPlanilla siguiente = estado.alCerrar();
        if (!validacion.esValida()) {
            String detalle = validacion.incidencias().stream().map(Incidencia::mensaje)
                    .collect(Collectors.joining("; "));
            throw new ReglaDePlanillaException("No se puede cerrar la planilla: " + detalle);
        }
        this.estado = siguiente;
        this.claveIdempotencia = clave;
        this.cerradaEn = ahora;
        return Optional.of(new PlanillaCerrada(id, empresaId, periodo, tipo, ahora, lineas.size(), totalNeto()));
    }

    public Dinero totalIngresos() {
        return sumar(LineaPlanilla::totalIngresos);
    }

    public Dinero totalDescuentos() {
        return sumar(LineaPlanilla::totalDescuentos);
    }

    public Dinero totalNeto() {
        return sumar(LineaPlanilla::neto);
    }

    public Dinero totalEssalud() {
        return sumar(LineaPlanilla::aporteEssalud);
    }

    private Dinero sumar(Function<LineaPlanilla, Dinero> campo) {
        return Dinero.suma(lineas.stream().map(campo).toList());
    }
}
