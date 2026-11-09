package pe.nexosoftware.planilla360.planilla.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Afp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.DescuentoPension;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

/**
 * Resultado de planilla de un trabajador: los datos de su boleta. La pensión y EsSalud se calculan sobre la
 * remuneración asegurable (básico + asignación familiar + horas extra + bonos), sin gratificación.
 */
public record LineaPlanilla(
        UUID empleadoId,
        Dinero basico,
        Dinero asignacionFamiliar,
        Dinero horasExtra,
        Dinero bonos,
        Dinero gratificacion,
        Dinero bonificacionExtraordinaria,
        Dinero cts,
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
        return basico.sumar(asignacionFamiliar).sumar(horasExtra).sumar(bonos);
    }

    public Dinero totalIngresos() {
        return remuneracionAsegurable().sumar(gratificacion).sumar(bonificacionExtraordinaria).sumar(cts);
    }

    public Dinero totalDescuentos() {
        return descuentoPension.total().sumar(retencion5ta).sumar(descuentoAsistencia).sumar(otrosDescuentos);
    }

    public Dinero neto() {
        return totalIngresos().restar(totalDescuentos());
    }

    /** Renglones de la boleta en orden de impresión; se omiten los conceptos sin monto. */
    public List<Concepto> conceptos() {
        List<Concepto> lista = new ArrayList<>();
        agregar(lista, "BASICO", "Remuneración básica", TipoConcepto.INGRESO, basico);
        agregar(lista, "ASIGNACION_FAMILIAR", "Asignación familiar", TipoConcepto.INGRESO, asignacionFamiliar);
        agregar(lista, "HORAS_EXTRA", "Horas extra", TipoConcepto.INGRESO, horasExtra);
        agregar(lista, "BONOS", "Bonos", TipoConcepto.INGRESO, bonos);
        agregar(lista, "GRATIFICACION", "Gratificación", TipoConcepto.INGRESO, gratificacion);
        agregar(lista, "BONIFICACION_EXTRAORDINARIA", "Bonificación extraordinaria (Ley 30334)",
                TipoConcepto.INGRESO, bonificacionExtraordinaria);
        agregar(lista, "CTS", "Compensación por tiempo de servicios (depósito)", TipoConcepto.INGRESO, cts);
        switch (regimenPensionario) {
            case Onp onp -> agregar(lista, "ONP", "Aporte ONP", TipoConcepto.DESCUENTO, descuentoPension.aporte());
            case Afp afp -> {
                agregar(lista, "AFP_APORTE", "Aporte obligatorio al fondo (" + afp.nombre() + ")",
                        TipoConcepto.DESCUENTO, descuentoPension.aporte());
                agregar(lista, "AFP_PRIMA", "Prima de seguro AFP", TipoConcepto.DESCUENTO, descuentoPension.prima());
                agregar(lista, "AFP_COMISION", "Comisión AFP", TipoConcepto.DESCUENTO, descuentoPension.comision());
            }
        }
        agregar(lista, "RENTA_5TA", "Retención de renta de 5.ª categoría", TipoConcepto.DESCUENTO, retencion5ta);
        agregar(lista, "TARDANZAS_FALTAS", "Tardanzas e inasistencias", TipoConcepto.DESCUENTO,
                descuentoAsistencia);
        agregar(lista, "OTROS_DESCUENTOS", "Otros descuentos", TipoConcepto.DESCUENTO, otrosDescuentos);
        agregar(lista, "ESSALUD", "EsSalud (aporte del empleador)", TipoConcepto.APORTE_EMPLEADOR, aporteEssalud);
        return List.copyOf(lista);
    }

    private static void agregar(List<Concepto> lista, String codigo, String descripcion, TipoConcepto tipo,
            Dinero monto) {
        if (!monto.esCero()) {
            lista.add(new Concepto(codigo, descripcion, tipo, monto));
        }
    }

    /** Builder de la boleta. */
    public static final class Builder {

        private final UUID empleadoId;
        private Dinero basico;
        private Dinero asignacionFamiliar = Dinero.CERO;
        private Dinero horasExtra = Dinero.CERO;
        private Dinero bonos = Dinero.CERO;
        private Gratificacion gratificacion;
        private Dinero cts = Dinero.CERO;
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

        public Builder cts(Dinero valor) {
            this.cts = valor;
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
            Objects.requireNonNull(empleadoId, "El trabajador es obligatorio");
            Objects.requireNonNull(basico, "El sueldo básico es obligatorio");
            Objects.requireNonNull(regimenPensionario, "El régimen pensionario es obligatorio");
            Objects.requireNonNull(parametros, "Los parámetros legales son obligatorios");
            exigirNoNegativo(basico, "El sueldo básico");
            exigirNoNegativo(asignacionFamiliar, "La asignación familiar");
            exigirNoNegativo(horasExtra, "Las horas extra");
            exigirNoNegativo(bonos, "Los bonos");
            exigirNoNegativo(cts, "La CTS");
            exigirNoNegativo(retencion5ta, "La retención de 5.ª categoría");
            exigirNoNegativo(descuentoAsistencia, "El descuento por asistencia");
            exigirNoNegativo(otrosDescuentos, "Los otros descuentos");

            Dinero asegurable = basico.sumar(asignacionFamiliar).sumar(horasExtra).sumar(bonos);
            Gratificacion semestral = gratificacion == null ? new Gratificacion(Dinero.CERO, Dinero.CERO)
                    : gratificacion;
            return new LineaPlanilla(empleadoId, basico, asignacionFamiliar, horasExtra, bonos, semestral.monto(),
                    semestral.bonificacionExtraordinaria(), cts, regimenPensionario,
                    regimenPensionario.calcular(asegurable, parametros), retencion5ta, descuentoAsistencia,
                    otrosDescuentos, asegurable.porcentaje(parametros.pctEssalud()));
        }

        private static void exigirNoNegativo(Dinero monto, String nombre) {
            if (monto.esNegativo()) {
                throw new IllegalArgumentException(nombre + " no puede ser negativo");
            }
        }
    }
}
