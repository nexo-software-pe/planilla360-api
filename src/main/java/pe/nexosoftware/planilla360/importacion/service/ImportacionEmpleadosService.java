package pe.nexosoftware.planilla360.importacion.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.nexosoftware.planilla360.catalogo.CatalogoApi;
import pe.nexosoftware.planilla360.compartido.dominio.ReglaDeDominioException;
import pe.nexosoftware.planilla360.compartido.dominio.Dni;
import pe.nexosoftware.planilla360.compartido.excepcion.PlanillaException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.NuevoEmpleado;
import pe.nexosoftware.planilla360.importacion.dto.ResultadoImportacion;
import pe.nexosoftware.planilla360.importacion.dto.ResultadoImportacion.ErrorFila;
import pe.nexosoftware.planilla360.importacion.lector.LectorHojaExcel;
import pe.nexosoftware.planilla360.importacion.lector.LectorHojaExcel.Fila;

/**
 * Importa empleados desde un Excel. Cada fila se valida por separado y todos los errores se reportan juntos, con su
 * número de fila y columna. Con {@code simular} solo se valida; al confirmar, se guardan las filas válidas (cada una
 * con su contrato, en su propia transacción) y las inválidas se omiten.
 */
@Service
public class ImportacionEmpleadosService {

    public static final int MAXIMO_FILAS = 500;
    public static final String[] COLUMNAS = {
        "dni", "nombres", "apellidos", "fecha_nacimiento", "email", "telefono", "fecha_ingreso", "area", "cargo",
        "tipo_contrato", "fecha_fin_contrato", "sueldo_basico", "jornada_semanal", "regimen_pensionario", "afp",
        "tipo_comision"
    };
    private static final Set<String> TIPOS_CONTRATO = Set.of("INDEFINIDO", "PLAZO_FIJO", "PART_TIME");
    private static final Set<String> COMISIONES = Set.of("FLUJO", "MIXTA");
    private static final long BYTES_MAXIMOS = 5L * 1024 * 1024;

    private final LectorHojaExcel lector;
    private final EmpleadoApi empleados;
    private final CatalogoApi catalogo;

    public ImportacionEmpleadosService(LectorHojaExcel lector, EmpleadoApi empleados, CatalogoApi catalogo) {
        this.lector = lector;
        this.empleados = empleados;
        this.catalogo = catalogo;
    }

    public ResultadoImportacion importar(MultipartFile archivo, boolean simular) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("Adjunta un archivo Excel (.xlsx).");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        if (!nombre.endsWith(".xlsx")) {
            throw new ReglaDeNegocioException("El archivo debe ser un Excel con extensión .xlsx.");
        }
        if (archivo.getSize() > BYTES_MAXIMOS) {
            throw new ReglaDeNegocioException("El archivo no puede pesar más de 5 MB.");
        }
        List<Fila> filas;
        try {
            filas = lector.leer(archivo.getInputStream(), MAXIMO_FILAS);
        } catch (IOException e) {
            throw new ReglaDeNegocioException("No se pudo leer el archivo.");
        } catch (IllegalArgumentException e) {
            throw new ReglaDeNegocioException(e.getMessage());
        }

        List<ErrorFila> errores = new ArrayList<>();
        int validas = 0;
        int importadas = 0;
        Set<String> dnisDelArchivo = new HashSet<>();
        for (Fila fila : filas) {
            List<ErrorFila> deLaFila = new ArrayList<>();
            NuevoEmpleado nuevo = validar(fila, dnisDelArchivo, deLaFila);
            if (!deLaFila.isEmpty()) {
                errores.addAll(deLaFila);
                continue;
            }
            validas++;
            if (simular) {
                continue;
            }
            try {
                empleados.registrar(nuevo);
                importadas++;
            } catch (PlanillaException | ReglaDeDominioException e) {
                validas--;
                errores.add(new ErrorFila(fila.numero(), null, e.getMessage()));
            }
        }
        return new ResultadoImportacion(simular, filas.size(), validas, importadas, List.copyOf(errores));
    }

    private NuevoEmpleado validar(Fila fila, Set<String> dnisDelArchivo, List<ErrorFila> errores) {
        Map<String, String> v = fila.valores();
        int n = fila.numero();

        String dni = requerido(v, "dni", n, errores);
        if (dni != null) {
            if (!Dni.esValido(dni)) {
                errores.add(new ErrorFila(n, "dni", "El DNI debe tener 8 dígitos."));
            } else if (!dnisDelArchivo.add(dni)) {
                errores.add(new ErrorFila(n, "dni", "El DNI " + dni + " está repetido en el archivo."));
            } else if (empleados.existeDni(dni)) {
                errores.add(new ErrorFila(n, "dni", "Ya existe un empleado con el DNI " + dni + "."));
            }
        }
        String nombres = requerido(v, "nombres", n, errores);
        String apellidos = requerido(v, "apellidos", n, errores);
        LocalDate nacimiento = fecha(v, "fecha_nacimiento", true, n, errores);
        LocalDate ingreso = fecha(v, "fecha_ingreso", true, n, errores);
        String email = v.get("email");
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errores.add(new ErrorFila(n, "email", "El correo no tiene un formato válido."));
        }

        UUID areaId = null;
        String area = requerido(v, "area", n, errores);
        if (area != null) {
            areaId = catalogo.areaIdPorNombre(area).orElse(null);
            if (areaId == null) {
                errores.add(new ErrorFila(n, "area", "No existe el área \"" + area + "\" en la empresa."));
            }
        }
        UUID cargoId = null;
        String cargo = requerido(v, "cargo", n, errores);
        if (cargo != null && areaId != null) {
            cargoId = catalogo.cargoIdPorNombre(areaId, cargo).orElse(null);
            if (cargoId == null) {
                errores.add(new ErrorFila(n, "cargo", "No existe el cargo \"" + cargo + "\" en el área " + area + "."));
            }
        }

        String tipoContrato = enumerado(v, "tipo_contrato", TIPOS_CONTRATO, true, n, errores);
        LocalDate finContrato = fecha(v, "fecha_fin_contrato", false, n, errores);
        if ("PLAZO_FIJO".equals(tipoContrato) && finContrato == null && !v.containsKey("fecha_fin_contrato")) {
            errores.add(new ErrorFila(n, "fecha_fin_contrato", "Un contrato a plazo fijo requiere fecha de fin."));
        }
        BigDecimal sueldo = decimal(v, "sueldo_basico", n, errores);
        Integer jornada = entero(v, "jornada_semanal", n, errores);
        if (jornada != null && (jornada < 1 || jornada > 48)) {
            errores.add(new ErrorFila(n, "jornada_semanal", "La jornada semanal debe estar entre 1 y 48 horas."));
        }

        String regimen = enumerado(v, "regimen_pensionario", Set.of("ONP", "AFP"), true, n, errores);
        UUID afpId = null;
        String comision = null;
        if ("AFP".equals(regimen)) {
            String afp = requerido(v, "afp", n, errores);
            if (afp != null) {
                afpId = catalogo.afpIdPorNombre(afp).orElse(null);
                if (afpId == null) {
                    errores.add(new ErrorFila(n, "afp", "No existe la AFP \"" + afp + "\"."));
                }
            }
            comision = enumerado(v, "tipo_comision", COMISIONES, true, n, errores);
        } else if ("ONP".equals(regimen) && (v.containsKey("afp") || v.containsKey("tipo_comision"))) {
            errores.add(new ErrorFila(n, "afp", "El régimen ONP no lleva AFP ni tipo de comisión."));
        }

        if (!errores.isEmpty()) {
            return null;
        }
        return new NuevoEmpleado(dni, nombres, apellidos, nacimiento, email, v.get("telefono"), ingreso, areaId,
                cargoId, tipoContrato, finContrato, sueldo, jornada, regimen, afpId, comision);
    }

    private static String requerido(Map<String, String> v, String campo, int fila, List<ErrorFila> errores) {
        String valor = v.get(campo);
        if (valor == null || valor.isBlank()) {
            errores.add(new ErrorFila(fila, campo, "Es obligatorio."));
            return null;
        }
        return valor.trim();
    }

    private static String enumerado(
            Map<String, String> v, String campo, Set<String> validos, boolean obligatorio, int fila,
            List<ErrorFila> errores) {
        String valor = obligatorio ? requerido(v, campo, fila, errores) : v.get(campo);
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase();
        if (!validos.contains(normalizado)) {
            errores.add(new ErrorFila(fila, campo, "Valor no válido; usa uno de " + new java.util.TreeSet<>(validos) + "."));
            return null;
        }
        return normalizado;
    }

    private static LocalDate fecha(
            Map<String, String> v, String campo, boolean obligatoria, int fila, List<ErrorFila> errores) {
        String valor = obligatoria ? requerido(v, campo, fila, errores) : v.get(campo);
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            errores.add(new ErrorFila(fila, campo, "La fecha debe tener el formato AAAA-MM-DD."));
            return null;
        }
    }

    private static BigDecimal decimal(Map<String, String> v, String campo, int fila, List<ErrorFila> errores) {
        String valor = requerido(v, campo, fila, errores);
        if (valor == null) {
            return null;
        }
        try {
            BigDecimal monto = new BigDecimal(valor.replace(",", ""));
            if (monto.signum() <= 0) {
                errores.add(new ErrorFila(fila, campo, "Debe ser mayor que cero."));
                return null;
            }
            return monto;
        } catch (NumberFormatException e) {
            errores.add(new ErrorFila(fila, campo, "Debe ser un número."));
            return null;
        }
    }

    private static Integer entero(Map<String, String> v, String campo, int fila, List<ErrorFila> errores) {
        String valor = requerido(v, campo, fila, errores);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            errores.add(new ErrorFila(fila, campo, "Debe ser un número entero."));
            return null;
        }
    }
}
