package pe.nexosoftware.planilla360.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** API pública del módulo de catálogos: parámetros legales vigentes y AFP. */
public interface CatalogoApi {

    /**
     * Valor del parámetro legal vigente en la fecha indicada (por ejemplo {@code RMV}, {@code UIT},
     * {@code ONP_PCT}). Los porcentajes se expresan como número (13 significa 13 %).
     */
    BigDecimal valorParametro(String codigo, LocalDate fecha);

    List<AfpResumen> afps();
}
