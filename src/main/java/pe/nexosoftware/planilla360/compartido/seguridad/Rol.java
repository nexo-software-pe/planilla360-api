package pe.nexosoftware.planilla360.compartido.seguridad;

public enum Rol {
    ADMIN_SAAS,
    ADMIN_EMPRESA,
    RRHH,
    EMPLEADO;

    public String authority() {
        return "ROLE_" + name();
    }
}
