package pe.nexosoftware.planilla360.integracion.cliente;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.integracion.model.ContribuyenteRuc;
import pe.nexosoftware.planilla360.integracion.model.PersonaDni;
import pe.nexosoftware.planilla360.integracion.model.TipoCambio;

/**
 * Adapter HTTP del servicio externo (WireMock en local). Traduce los errores: 404 es "no existe", otro 4xx es una
 * petición inválida y 5xx, timeouts y fallos de red son transitorios (la resiliencia los reintenta).
 */
@Component
@ConditionalOnProperty(name = "app.integracion.modo", havingValue = "http")
class HttpProveedorConsultas implements ProveedorConsultas {

    private final RestClient cliente;

    HttpProveedorConsultas(IntegracionProperties propiedades) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(propiedades.timeout()).build());
        fabrica.setReadTimeout(propiedades.timeout());
        this.cliente = RestClient.builder().baseUrl(propiedades.baseUrl()).requestFactory(fabrica).build();
    }

    @Override
    public PersonaDni consultarDni(String dni) {
        return obtener("DNI " + dni, () -> cliente.get().uri("/dni/{dni}", dni), PersonaDni.class);
    }

    @Override
    public ContribuyenteRuc consultarRuc(String ruc) {
        return obtener("RUC " + ruc, () -> cliente.get().uri("/ruc/{ruc}", ruc), ContribuyenteRuc.class);
    }

    @Override
    public TipoCambio consultarTipoCambio() {
        TipoCambioExterno externo =
                obtener("tipo de cambio", () -> cliente.get().uri("/tipo-cambio"), TipoCambioExterno.class);
        return new TipoCambio(externo.fecha(), externo.moneda(), externo.compra(), externo.venta(), false);
    }

    private static <T> T obtener(
            String recurso, Supplier<RestClient.RequestHeadersSpec<?>> peticion, Class<T> tipo) {
        try {
            T resultado = peticion.get().retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> rechazar(res.getStatusCode(), recurso))
                    .body(tipo);
            if (resultado == null) {
                throw new ErrorTransitorioException("El servicio externo respondió vacío", null);
            }
            return resultado;
        } catch (RecursoNoEncontradoException | ReglaDeNegocioException | ErrorTransitorioException e) {
            throw e;
        } catch (RestClientException e) {
            // 5xx, timeout o red caída.
            throw new ErrorTransitorioException("Falló la llamada al servicio externo: " + e.getMessage(), e);
        }
    }

    private static void rechazar(HttpStatusCode estado, String recurso) {
        if (estado.value() == 404) {
            throw new RecursoNoEncontradoException("No se encontró el " + recurso + ".");
        }
        throw new ReglaDeNegocioException("El servicio externo rechazó la consulta de " + recurso + ".");
    }

    private record TipoCambioExterno(LocalDate fecha, String moneda, BigDecimal compra, BigDecimal venta) {
    }
}
