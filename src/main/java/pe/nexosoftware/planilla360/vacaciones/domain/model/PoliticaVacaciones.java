package pe.nexosoftware.planilla360.vacaciones.domain.model;

/**
 * Strategy: reglas de vacaciones según el régimen laboral de la empresa. (Esqueleto del commit ROJO.)
 */
public sealed interface PoliticaVacaciones permits PoliticaVacaciones.General, PoliticaVacaciones.PequenaEmpresa {

    int diasPorAnio();

    int maximoDiasVendibles();

    static PoliticaVacaciones general() {
        throw new UnsupportedOperationException("pendiente");
    }

    static PoliticaVacaciones pequenaEmpresa() {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Régimen general: 30 días al año; se pueden vender hasta 15. */
    record General() implements PoliticaVacaciones {

        @Override
        public int diasPorAnio() {
            throw new UnsupportedOperationException("pendiente");
        }

        @Override
        public int maximoDiasVendibles() {
            throw new UnsupportedOperationException("pendiente");
        }
    }

    /** Pequeña empresa: 15 días al año; no se pueden vender. */
    record PequenaEmpresa() implements PoliticaVacaciones {

        @Override
        public int diasPorAnio() {
            throw new UnsupportedOperationException("pendiente");
        }

        @Override
        public int maximoDiasVendibles() {
            throw new UnsupportedOperationException("pendiente");
        }
    }
}
