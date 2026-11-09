package pe.nexosoftware.planilla360.boleta.pdf;

/** Puerto de salida: convierte los datos de una boleta en un documento PDF (patrón Adapter). */
public interface GeneradorBoletaPdf {

    byte[] generar(DatosBoletaPdf datos);
}
