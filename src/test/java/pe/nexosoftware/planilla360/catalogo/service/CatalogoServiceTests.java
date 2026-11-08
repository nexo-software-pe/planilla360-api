package pe.nexosoftware.planilla360.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalResponse;
import pe.nexosoftware.planilla360.catalogo.mapper.CatalogoMapper;
import pe.nexosoftware.planilla360.catalogo.repository.AfpRepository;
import pe.nexosoftware.planilla360.catalogo.repository.AreaRepository;
import pe.nexosoftware.planilla360.catalogo.repository.CargoRepository;
import pe.nexosoftware.planilla360.catalogo.repository.FeriadoRepository;
import pe.nexosoftware.planilla360.catalogo.repository.ParametroLegalRepository;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;

class CatalogoServiceTests {

    private final CatalogoCache cache = mock(CatalogoCache.class);
    private final CatalogoService service = new CatalogoService(
            cache, mock(ParametroLegalRepository.class), mock(FeriadoRepository.class),
            mock(AreaRepository.class), mock(CargoRepository.class), mock(AfpRepository.class),
            mock(CatalogoMapper.class), mock(ApplicationEventPublisher.class));

    private static ParametroLegalResponse rmv(String valor, String desde, String hasta) {
        return new ParametroLegalResponse(UUID.randomUUID(), "RMV", "RMV", new BigDecimal(valor),
                LocalDate.parse(desde), hasta == null ? null : LocalDate.parse(hasta));
    }

    @Test
    void devuelveElValorVigenteEnLaFechaConsultada() {
        when(cache.parametros()).thenReturn(List.of(
                rmv("1025.00", "2022-05-01", "2024-12-31"),
                rmv("1130.00", "2025-01-01", "2026-12-31"),
                rmv("1200.00", "2027-01-01", null)));

        assertThat(service.valorParametro("RMV", LocalDate.parse("2024-06-15"))).isEqualByComparingTo("1025.00");
        assertThat(service.valorParametro("RMV", LocalDate.parse("2026-12-31"))).isEqualByComparingTo("1130.00");
        assertThat(service.valorParametro("RMV", LocalDate.parse("2027-01-01"))).isEqualByComparingTo("1200.00");
    }

    @Test
    void fechaAnteriorALaPrimeraVigenciaNoTieneValor() {
        when(cache.parametros()).thenReturn(List.of(rmv("1130.00", "2025-01-01", null)));

        assertThatThrownBy(() -> service.valorParametro("RMV", LocalDate.parse("2020-01-01")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void parametroDesconocidoLanzaNoEncontrado() {
        when(cache.parametros()).thenReturn(List.of(rmv("1130.00", "2025-01-01", null)));

        assertThatThrownBy(() -> service.valorParametro("XYZ", LocalDate.parse("2026-01-01")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
