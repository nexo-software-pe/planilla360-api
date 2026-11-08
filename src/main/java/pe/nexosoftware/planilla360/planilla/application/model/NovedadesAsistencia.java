package pe.nexosoftware.planilla360.planilla.application.model;

/** Tardanzas y faltas injustificadas de un trabajador en el mes. */
public record NovedadesAsistencia(int minutosTardanza, int faltasInjustificadas) {

    public static final NovedadesAsistencia NINGUNA = new NovedadesAsistencia(0, 0);
}
