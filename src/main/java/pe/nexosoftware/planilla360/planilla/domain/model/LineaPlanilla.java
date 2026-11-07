package pe.nexosoftware.planilla360.planilla.domain.model;

import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.DescuentoPension;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

/** Resultado de planilla de un trabajador (los datos de su boleta). Esqueleto: se construye test-first. */
public record LineaPlanilla(
        UUID empleadoId,
        Dinero basico,
        Dinero asignacionFamiliar,
        Dinero horasExtra,
        Dinero bonos,
        Dinero gratificacion,
        Dinero bonificacionExtraordinaria,
        RegimenPensionario regimenPensionario,
        DescuentoPension descuentoPension,
        Dinero retencion5ta,
        Dinero descuentoAsistencia,
        Dinero otrosDescuentos,
        Dinero aporteEssalud) {

    public static Builder de(UUID empleadoId) {
        return new Builder(empleadoId);
    }

    public Dinero remuneracionAsegurable() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero totalIngresos() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero totalDescuentos() {
        throw new UnsupportedOperationException("pendiente");
    }

    public Dinero neto() {
        throw new UnsupportedOperationException("pendiente");
    }

    public List<Concepto> conceptos() {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Builder de la boleta. */
    public static final class Builder {

        private final UUID empleadoId;
        private Dinero basico;
        private Dinero asignacionFamiliar = Dinero.CERO;
        private Dinero horasExtra = Dinero.CERO;
        private Dinero bonos = Dinero.CERO;
        private Gratificacion gratificacion;
        private RegimenPensionario regimenPensionario;
        private Dinero retencion5ta = Dinero.CERO;
        private Dinero descuentoAsistencia = Dinero.CERO;
        private Dinero otrosDescuentos = Dinero.CERO;

        private Builder(UUID empleadoId) {
            this.empleadoId = empleadoId;
        }

        public Builder basico(Dinero valor) {
            this.basico = valor;
            return this;
        }

        public Builder asignacionFamiliar(Dinero valor) {
            this.asignacionFamiliar = valor;
            return this;
        }

        public Builder horasExtra(Dinero valor) {
            this.horasExtra = valor;
            return this;
        }

        public Builder bonos(Dinero valor) {
            this.bonos = valor;
            return this;
        }

        public Builder gratificacion(Gratificacion valor) {
            this.gratificacion = valor;
            return this;
        }

        public Builder regimenPensionario(RegimenPensionario valor) {
            this.regimenPensionario = valor;
            return this;
        }

        public Builder retencion5ta(Dinero valor) {
            this.retencion5ta = valor;
            return this;
        }

        public Builder descuentoAsistencia(Dinero valor) {
            this.descuentoAsistencia = valor;
            return this;
        }

        public Builder otrosDescuentos(Dinero valor) {
            this.otrosDescuentos = valor;
            return this;
        }

        public LineaPlanilla build(ParametrosLegales parametros) {
            throw new UnsupportedOperationException("pendiente");
        }
    }
}
