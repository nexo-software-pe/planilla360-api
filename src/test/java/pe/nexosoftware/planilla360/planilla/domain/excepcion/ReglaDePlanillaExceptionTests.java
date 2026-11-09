package pe.nexosoftware.planilla360.planilla.domain.excepcion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.ReglaDeDominioException;

@DisplayName("ReglaDePlanillaException")
class ReglaDePlanillaExceptionTests {

    @Test
    void esUnaReglaDeDominioParaQueLaCapaWebLaTraduzcaA422() {
        ReglaDePlanillaException error = new ReglaDePlanillaException("no se puede");

        assertThat(error).isInstanceOf(ReglaDeDominioException.class).hasMessage("no se puede");
    }
}
