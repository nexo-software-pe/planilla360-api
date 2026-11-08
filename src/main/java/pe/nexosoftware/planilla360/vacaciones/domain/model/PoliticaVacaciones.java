package pe.nexosoftware.planilla360.vacaciones.domain.model;

/**
 * Strategy: reglas de vacaciones según el régimen laboral de la empresa. Es una interfaz sellada: el
 * compilador sabe que solo hay dos políticas.
 */
public sealed interface PoliticaVacaciones permits PoliticaVacaciones.General, PoliticaVacaciones.PequenaEmpresa {

    int diasPorAnio();

    int maximoDiasVendibles();

    static PoliticaVacaciones general() {
        return new General();
    }

    static PoliticaVacaciones pequenaEmpresa() {
        return new PequenaEmpresa();
    }

    /** Régimen general: 30 días al año; se pueden vender hasta 15. */
    record General() implements PoliticaVacaciones {

        @Override
        public int diasPorAnio() {
            return 30;
        }

        @Override
        public int maximoDiasVendibles() {
            return 15;
        }
    }

    /** Pequeña empresa: 15 días al año; no se pueden vender. */
    record PequenaEmpresa() implements PoliticaVacaciones {

        @Override
        public int diasPorAnio() {
            return 15;
        }

        @Override
        public int maximoDiasVendibles() {
            return 0;
        }
    }
}
