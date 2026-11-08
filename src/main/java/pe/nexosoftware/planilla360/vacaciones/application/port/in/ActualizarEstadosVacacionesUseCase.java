package pe.nexosoftware.planilla360.vacaciones.application.port.in;

public interface ActualizarEstadosVacacionesUseCase {

    record Resultado(int iniciadas, int finalizadas) {
    }

    /**
     * Pasa a "en goce" las vacaciones que empiezan hoy o antes y finaliza las que terminaron, descontando
     * los días gozados del periodo. Se ejecuta para la empresa del contexto.
     */
    Resultado actualizar();
}
