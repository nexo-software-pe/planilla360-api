package pe.nexosoftware.planilla360.boleta.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Todo lo que lleva la boleta impresa; el generador de PDF no consulta nada más. */
public record DatosBoletaPdf(
        String empresaRazonSocial,
        String empresaRuc,
        String titulo,
        String periodo,
        String trabajador,
        String dni,
        String cargo,
        String area,
        LocalDate fechaIngreso,
        String regimenPensionario,
        List<Renglon> ingresos,
        List<Renglon> descuentos,
        List<Renglon> aportesEmpleador,
        BigDecimal totalIngresos,
        BigDecimal totalDescuentos,
        BigDecimal neto) {

    public record Renglon(String descripcion, BigDecimal monto) {
    }
}
