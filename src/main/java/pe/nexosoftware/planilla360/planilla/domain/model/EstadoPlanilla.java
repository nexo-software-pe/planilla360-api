package pe.nexosoftware.planilla360.planilla.domain.model;

import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

/**
 * Ciclo de vida de una planilla (patrón State): BORRADOR → CALCULADA → CERRADA, y ANULADA como salida definitiva.
 * Las validaciones previas al cierre NO son un estado: son el resultado de revisar la planilla.
 */
public enum EstadoPlanilla {

    BORRADOR {
        @Override
        public EstadoPlanilla alCalcular() {
            return CALCULADA;
        }

        @Override
        public EstadoPlanilla alAnular() {
            return ANULADA;
        }

        @Override
        public boolean permiteModificarLineas() {
            return true;
        }
    },

    CALCULADA {
        @Override
        public EstadoPlanilla alCalcular() {
            return CALCULADA;
        }

        @Override
        public EstadoPlanilla alCerrar() {
            return CERRADA;
        }

        @Override
        public EstadoPlanilla alAnular() {
            return ANULADA;
        }

        @Override
        public boolean permiteModificarLineas() {
            return true;
        }
    },

    CERRADA {
        @Override
        public EstadoPlanilla alAnular() {
            return ANULADA;
        }
    },

    ANULADA;

    public EstadoPlanilla alCalcular() {
        throw transicionInvalida("calcular");
    }

    public EstadoPlanilla alCerrar() {
        throw transicionInvalida("cerrar");
    }

    public EstadoPlanilla alAnular() {
        throw transicionInvalida("anular");
    }

    public boolean permiteModificarLineas() {
        return false;
    }

    private ReglaDePlanillaException transicionInvalida(String accion) {
        return new ReglaDePlanillaException("No se puede " + accion + " una planilla en estado " + name());
    }
}
