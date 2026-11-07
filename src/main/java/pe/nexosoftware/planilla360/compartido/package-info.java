/**
 * Kernel compartido: contexto de tenant, seguridad, excepciones, value objects y utilidades.
 * Es un módulo abierto: el resto de módulos puede usar todos sus paquetes.
 */
@ApplicationModule(displayName = "Compartido", type = ApplicationModule.Type.OPEN)
package pe.nexosoftware.planilla360.compartido;

import org.springframework.modulith.ApplicationModule;
