package pe.nexosoftware.planilla360.boleta;

import java.util.UUID;

/** API pública del módulo de boletas, para quien necesita dejar constancia de lo ocurrido con una boleta. */
public interface BoletaApi {

    /** Marca la boleta como enviada por correo al trabajador (la empresa de la boleta debe ser la del contexto). */
    void marcarEnviada(UUID boletaId);
}
