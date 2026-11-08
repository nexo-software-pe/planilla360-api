package pe.nexosoftware.planilla360.empleado.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import pe.nexosoftware.planilla360.empleado.dto.FiltroEmpleados;
import pe.nexosoftware.planilla360.empleado.entity.Empleado;
import pe.nexosoftware.planilla360.empleado.model.EstadoEmpleado;

/**
 * Patrón Specification: cada criterio de búsqueda es una pieza pequeña y reutilizable; el filtro
 * completo se arma combinándolas con {@code and}. Un criterio nulo no restringe nada.
 */
final class EmpleadoSpecs {

    private EmpleadoSpecs() {
    }

    static Specification<Empleado> desde(FiltroEmpleados filtro) {
        return Specification.<Empleado>unrestricted()
                .and(textoLibre(filtro.q()))
                .and(igual("areaId", filtro.areaId()))
                .and(igual("cargoId", filtro.cargoId()))
                .and(igual("jefeId", filtro.jefeId()))
                .and(estado(filtro.estado()))
                .and(ingresoDesde(filtro.ingresoDesde()))
                .and(ingresoHasta(filtro.ingresoHasta()));
    }

    /** Coincide con el DNI o con "nombres apellidos" sin distinguir mayúsculas. */
    static Specification<Empleado> textoLibre(String q) {
        if (q == null || q.isBlank()) {
            return Specification.unrestricted();
        }
        String patron = "%" + escapar(q.trim().toLowerCase(Locale.ROOT)) + "%";
        return (raiz, consulta, cb) -> cb.or(
                cb.like(raiz.get("dni"), patron, '\\'),
                cb.like(cb.lower(cb.concat(cb.concat(raiz.get("nombres"), " "), raiz.get("apellidos"))), patron, '\\'));
    }

    static Specification<Empleado> igual(String atributo, UUID valor) {
        if (valor == null) {
            return Specification.unrestricted();
        }
        return (raiz, consulta, cb) -> cb.equal(raiz.get(atributo), valor);
    }

    static Specification<Empleado> estado(EstadoEmpleado estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado);
    }

    static Specification<Empleado> ingresoDesde(LocalDate fecha) {
        if (fecha == null) {
            return Specification.unrestricted();
        }
        return (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("fechaIngreso"), fecha);
    }

    static Specification<Empleado> ingresoHasta(LocalDate fecha) {
        if (fecha == null) {
            return Specification.unrestricted();
        }
        return (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("fechaIngreso"), fecha);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
