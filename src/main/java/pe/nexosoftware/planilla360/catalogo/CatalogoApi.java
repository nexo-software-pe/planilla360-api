package pe.nexosoftware.planilla360.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de catálogos: parámetros legales vigentes y AFP. */
public interface CatalogoApi {

    /**
     * Valor del parámetro legal vigente en la fecha indicada (por ejemplo {@code RMV}, {@code UIT},
     * {@code ONP_PCT}). Los porcentajes se expresan como número (13 significa 13 %).
     */
    BigDecimal valorParametro(String codigo, LocalDate fecha);

    /** Como {@link #valorParametro}, pero devuelve vacío en vez de fallar cuando no hay valor vigente. */
    Optional<BigDecimal> parametroVigente(String codigo, LocalDate fecha);

    List<AfpResumen> afps();

    /** Indica si el área existe en la empresa del contexto. */
    boolean existeArea(UUID id);

    /** Indica si el cargo existe en la empresa del contexto. */
    boolean existeCargo(UUID id);

    boolean existeAfp(UUID id);

    boolean esFeriado(LocalDate fecha);

    /** Área de la empresa del contexto por su nombre (sin distinguir mayúsculas). */
    Optional<UUID> areaIdPorNombre(String nombre);

    /** Cargo de la empresa del contexto por su nombre dentro del área indicada (sin distinguir mayúsculas). */
    Optional<UUID> cargoIdPorNombre(UUID areaId, String nombre);

    Optional<String> nombreDeArea(UUID id);

    Optional<String> nombreDeCargo(UUID id);

    /** AFP por su nombre (sin distinguir mayúsculas). */
    Optional<UUID> afpIdPorNombre(String nombre);
}
