package pe.nexosoftware.planilla360.planilla.domain.model;

import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;

/** Agregado Planilla de una empresa y un periodo. Esqueleto: se construye test-first. */
public final class Planilla {

    private Planilla() {
    }

    public static Planilla crearBorrador(UUID id, UUID empresaId, Periodo periodo, TipoPlanilla tipo) {
        throw new UnsupportedOperationException("pendiente");
    }

    public UUID id() {
        throw new UnsupportedOperationException("pendiente");
    }

    public UUID empresaId() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Periodo periodo() {
        throw new UnsupportedOperationException("pendiente");
    }

    public TipoPlanilla tipo() {
        throw new UnsupportedOperationException("pendiente");
    }

    public EstadoPlanilla estado() {
        throw new UnsupportedOperationException("pendiente");
    }

    public List<LineaPlanilla> lineas() {
        throw new UnsupportedOperationException("pendiente");
    }

    public void registrarCalculo(List<LineaPlanilla> lineas) {
        throw new UnsupportedOperationException("pendiente");
    }

    public void anular() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero totalIngresos() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero totalDescuentos() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero totalNeto() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero totalEssalud() {
        throw new UnsupportedOperationException("pendiente");
    }
}
