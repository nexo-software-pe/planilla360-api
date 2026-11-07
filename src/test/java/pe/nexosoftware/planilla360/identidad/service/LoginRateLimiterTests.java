package pe.nexosoftware.planilla360.identidad.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.excepcion.DemasiadosIntentosException;

class LoginRateLimiterTests {

    private final LoginRateLimiter limiter = new LoginRateLimiter(new RateLimitProperties(3, 100));

    @Test
    void bloqueaDespuesDelLimitePorCredencial() {
        for (int i = 0; i < 3; i++) {
            limiter.verificar("10.0.0.1", "a@ejemplo.pe");
        }
        assertThatThrownBy(() -> limiter.verificar("10.0.0.1", "A@ejemplo.pe"))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(e -> org.assertj.core.api.Assertions
                        .assertThat(((DemasiadosIntentosException) e).reintentarEnSegundos()).isPositive());
    }

    @Test
    void otroCorreoOOtraIpTienenCuposIndependientes() {
        for (int i = 0; i < 3; i++) {
            limiter.verificar("10.0.0.1", "a@ejemplo.pe");
        }
        assertThatCode(() -> limiter.verificar("10.0.0.1", "b@ejemplo.pe")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.verificar("10.0.0.2", "a@ejemplo.pe")).doesNotThrowAnyException();
    }

    @Test
    void tambienLimitaPorIpAunqueCambieElCorreo() {
        LoginRateLimiter estricto = new LoginRateLimiter(new RateLimitProperties(10, 2));
        estricto.verificar("10.0.0.9", "a@ejemplo.pe");
        estricto.verificar("10.0.0.9", "b@ejemplo.pe");
        assertThatThrownBy(() -> estricto.verificar("10.0.0.9", "c@ejemplo.pe"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }
}
