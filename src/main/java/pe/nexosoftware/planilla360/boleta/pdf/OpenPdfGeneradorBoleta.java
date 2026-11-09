package pe.nexosoftware.planilla360.boleta.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.boleta.pdf.DatosBoletaPdf.Renglon;

/** Adapter de {@link GeneradorBoletaPdf} sobre OpenPDF. */
@Component
class OpenPdfGeneradorBoleta implements GeneradorBoletaPdf {

    private static final Color AZUL = new Color(0x1F, 0x3A, 0x5F);
    private static final Color GRIS = new Color(0xEE, 0xF0, 0xF3);
    private static final Color BORDE = new Color(0xD5, 0xD9, 0xE0);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, AZUL);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL);
    private static final Font DOCUMENTO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

    @Override
    public byte[] generar(DatosBoletaPdf datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Boleta de pago " + datos.periodo() + " - " + datos.trabajador());
            documento.addAuthor(datos.empresaRazonSocial());
            documento.open();
            documento.add(new Paragraph(datos.empresaRazonSocial(), TITULO));
            documento.add(new Paragraph("RUC " + datos.empresaRuc(), SUBTITULO));
            Paragraph titulo = new Paragraph(datos.titulo() + " - " + datos.periodo(), DOCUMENTO);
            titulo.setSpacingBefore(12);
            titulo.setSpacingAfter(8);
            documento.add(titulo);
            documento.add(datosDelTrabajador(datos));
            documento.add(espacio());
            documento.add(columnas(datos));
            documento.add(espacio());
            documento.add(totales(datos));
            if (!datos.aportesEmpleador().isEmpty()) {
                documento.add(espacio());
                documento.add(aportes(datos.aportesEmpleador()));
            }
            Paragraph pie = new Paragraph(
                    "Documento generado por Planilla360. Importes en soles (PEN).", SUBTITULO);
            pie.setSpacingBefore(24);
            documento.add(pie);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo armar el PDF de la boleta", e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private static PdfPTable datosDelTrabajador(DatosBoletaPdf d) {
        PdfPTable tabla = new PdfPTable(new float[] {1.2f, 2.8f, 1.2f, 2.8f});
        tabla.setWidthPercentage(100);
        fila(tabla, "Trabajador", d.trabajador(), "DNI", d.dni());
        fila(tabla, "Cargo", d.cargo(), "Área", d.area());
        fila(tabla, "Ingreso", String.valueOf(d.fechaIngreso()), "Régimen pensionario", d.regimenPensionario());
        return tabla;
    }

    private static void fila(PdfPTable tabla, String e1, String v1, String e2, String v2) {
        tabla.addCell(celda(e1, ETIQUETA, Element.ALIGN_LEFT, null));
        tabla.addCell(celda(v1, TEXTO, Element.ALIGN_LEFT, null));
        tabla.addCell(celda(e2, ETIQUETA, Element.ALIGN_LEFT, null));
        tabla.addCell(celda(v2, TEXTO, Element.ALIGN_LEFT, null));
    }

    private static PdfPTable columnas(DatosBoletaPdf d) {
        PdfPTable tabla = new PdfPTable(new float[] {1, 1});
        tabla.setWidthPercentage(100);
        for (PdfPTable detalle : List.of(detalle("Ingresos", d.ingresos()), detalle("Descuentos", d.descuentos()))) {
            PdfPCell contenedor = new PdfPCell(detalle);
            contenedor.setBorder(0);
            contenedor.setPadding(2);
            contenedor.setVerticalAlignment(Element.ALIGN_TOP);
            tabla.addCell(contenedor);
        }
        return tabla;
    }

    private static PdfPTable detalle(String titulo, List<Renglon> renglones) {
        PdfPTable tabla = new PdfPTable(new float[] {3, 1});
        tabla.setWidthPercentage(100);
        PdfPCell cabecera = celda(titulo, ENCABEZADO, Element.ALIGN_LEFT, AZUL);
        cabecera.setColspan(2);
        tabla.addCell(cabecera);
        for (Renglon r : renglones) {
            tabla.addCell(celda(r.descripcion(), TEXTO, Element.ALIGN_LEFT, null));
            tabla.addCell(celda(dinero(r.monto()), TEXTO, Element.ALIGN_RIGHT, null));
        }
        if (renglones.isEmpty()) {
            PdfPCell vacio = celda("-", TEXTO, Element.ALIGN_LEFT, null);
            vacio.setColspan(2);
            tabla.addCell(vacio);
        }
        return tabla;
    }

    private static PdfPTable totales(DatosBoletaPdf d) {
        PdfPTable tabla = new PdfPTable(new float[] {3, 1});
        tabla.setWidthPercentage(60);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(celda("Total ingresos", ETIQUETA, Element.ALIGN_LEFT, GRIS));
        tabla.addCell(celda(dinero(d.totalIngresos()), TEXTO, Element.ALIGN_RIGHT, GRIS));
        tabla.addCell(celda("Total descuentos", ETIQUETA, Element.ALIGN_LEFT, GRIS));
        tabla.addCell(celda(dinero(d.totalDescuentos()), TEXTO, Element.ALIGN_RIGHT, GRIS));
        tabla.addCell(celda("NETO A PAGAR", TOTAL, Element.ALIGN_LEFT, null));
        tabla.addCell(celda(dinero(d.neto()), TOTAL, Element.ALIGN_RIGHT, null));
        return tabla;
    }

    private static PdfPTable aportes(List<Renglon> renglones) {
        PdfPTable tabla = detalle("Aportes del empleador (informativo)", renglones);
        tabla.setWidthPercentage(50);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        return tabla;
    }

    private static Paragraph espacio() {
        Paragraph p = new Paragraph(" ");
        p.setLeading(8);
        return p;
    }

    private static PdfPCell celda(String texto, Font fuente, int alineacion, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        celda.setBorderColor(BORDE);
        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }
        return celda;
    }

    private static String dinero(BigDecimal monto) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(monto);
    }
}
