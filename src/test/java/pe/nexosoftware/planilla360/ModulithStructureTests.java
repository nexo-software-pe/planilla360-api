package pe.nexosoftware.planilla360;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTests {

    @Test
    void estructuraDeModulosEsValida() {
        ApplicationModules.of(Planilla360ApiApplication.class).verify();
    }
}
