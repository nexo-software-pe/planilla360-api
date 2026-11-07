package pe.nexosoftware.planilla360.compartido.validacion;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pe.nexosoftware.planilla360.compartido.dominio.Ruc;

public class RucValidoValidator implements ConstraintValidator<RucValido, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        return valor == null || Ruc.esValido(valor);
    }
}
