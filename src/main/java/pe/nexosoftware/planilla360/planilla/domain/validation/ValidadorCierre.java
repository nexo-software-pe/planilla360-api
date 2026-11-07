package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.Objects;

/** Cadena estándar de validaciones previas al cierre, en el orden en que se muestran al usuario. */
public final class ValidadorCierre {

    private final EslabonValidacion cadena;

    private ValidadorCierre(EslabonValidacion cadena) {
        this.cadena = Objects.requireNonNull(cadena);
    }

    public static ValidadorCierre estandar() {
        EslabonValidacion primero = new TrabajadoresConRegimenPensionario();
        primero.luego(new AsistenciaDelMesCompleta())
                .luego(new SinVacacionesPendientes())
                .luego(new SinOtraPlanillaCerrada())
                .luego(new NetosNoNegativos());
        return new ValidadorCierre(primero);
    }

    public ResultadoValidacion validar(ContextoCierre contexto) {
        return new ResultadoValidacion(cadena.validar(contexto));
    }
}
