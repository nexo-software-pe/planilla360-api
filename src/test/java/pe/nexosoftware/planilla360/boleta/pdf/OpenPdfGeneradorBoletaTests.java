package pe.nexosoftware.planilla360.boleta.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.boleta.pdf.DatosBoletaPdf.Renglon;

@DisplayName("Generador de PDF de boletas")
class OpenPdfGeneradorBoletaTests {

    private final GeneradorBoletaPdf generador = new OpenPdfGeneradorBoleta();

    private static DatosBoletaPdf datos(List<Renglon> aportes) {
        return new DatosBoletaPdf("Textiles Andinos S.A.C.", "20601234565", "Boleta de pago de remuneraciones",
                "2026-10", "Flores Paredes, Lucía", "40125001", "Ejecutiva de ventas", "Ventas",
                LocalDate.of(2023, 2, 1), "AFP Integra",
                List.of(new Renglon("Remuneración básica", new BigDecimal("2200.00")),
                        new Renglon("Horas extra", new BigDecimal("96.25"))),
                List.of(new Renglon("Comisión AFP", new BigDecimal("32.10"))),
                aportes,
                new BigDecimal("2296.25"), new BigDecimal("32.10"), new BigDecimal("2264.15"));
    }

    private static String textoDe(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(lector).getTextFromPage(1);
        } finally {
            lector.close();
        }
    }

    @Test
    @DisplayName("produce un PDF válido con los datos del trabajador, los conceptos y el neto")
    void pdfConDatos() throws Exception {
        byte[] pdf = generador.generar(datos(List.of(new Renglon("EsSalud", new BigDecimal("206.66")))));

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String texto = textoDe(pdf);
        assertThat(texto).contains("Textiles Andinos S.A.C.", "20601234565", "Flores Paredes, Lucía", "40125001",
                "Remuneración básica", "2,200.00", "Comisión AFP", "NETO A PAGAR", "2,264.15", "EsSalud");
    }

    @Test
    @DisplayName("funciona sin aportes del empleador ni conceptos en una columna")
    void sinAportes() throws Exception {
        DatosBoletaPdf sinDescuentos = new DatosBoletaPdf("Empresa", "20601234565", "Boleta", "2026-10", "Trabajador",
                "40125001", "Cargo", "Área", LocalDate.of(2023, 2, 1), "ONP",
                List.of(new Renglon("Remuneración básica", new BigDecimal("1300.00"))), List.of(), List.of(),
                new BigDecimal("1300.00"), BigDecimal.ZERO, new BigDecimal("1300.00"));

        assertThat(textoDe(generador.generar(sinDescuentos))).contains("1,300.00");
    }
}
