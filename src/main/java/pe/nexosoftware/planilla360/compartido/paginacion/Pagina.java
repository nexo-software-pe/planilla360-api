package pe.nexosoftware.planilla360.compartido.paginacion;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Respuesta paginada estable de la API (no se expone el objeto Page de Spring directamente). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> conversor) {
        return new Pagina<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
