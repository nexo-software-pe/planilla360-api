package pe.nexosoftware.planilla360.planilla.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

/** Agregado Planilla de una empresa y un periodo: sus líneas solo cambian mientras no esté cerrada. */
public final class Planilla {

    private final UUID id;
    private final UUID empresaId;
    private final Periodo periodo;
    private final TipoPlanilla tipo;
    private EstadoPlanilla estado = EstadoPlanilla.BORRADOR;
    private List<LineaPlanilla> lineas = List.of();

    private Planilla(UUID id, UUID empresaId, Periodo periodo, TipoPlanilla tipo) {
        this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
        this.empresaId = Objects.requireNonNull(empresaId, "La empresa es obligatoria");
        this.periodo = Objects.requireNonNull(periodo, "El periodo es obligatorio");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de planilla es obligatorio");
    }

    public static Planilla crearBorrador(UUID id, UUID empresaId, Periodo periodo, TipoPlanilla tipo) {
        return new Planilla(id, empresaId, periodo, tipo);
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
