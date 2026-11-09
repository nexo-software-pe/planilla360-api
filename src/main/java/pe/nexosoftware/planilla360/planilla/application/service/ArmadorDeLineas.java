package pe.nexosoftware.planilla360.planilla.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.model.NovedadesAsistencia;
import pe.nexosoftware.planilla360.planilla.application.model.SobretasasHorasExtra;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaAsistenciaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaCatalogoPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaEmpresaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaPersonalPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaVacacionesPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.HistorialPlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.service.PersonalDelPeriodo.TrabajadorConRegimen;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosBeneficioSemestral;
import pe.nexosoftware.planilla360.planilla.domain.model.DatosMensualesTrabajador;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;
import pe.nexosoftware.planilla360.planilla.domain.model.SemestreDeBeneficio;
import pe.nexosoftware.planilla360.planilla.domain.model.TiempoComputable;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenLaboral;
import pe.nexosoftware.planilla360.planilla.domain.service.CalculadoraLineaMensual;
import pe.nexosoftware.planilla360.planilla.domain.service.CalculadoraLineaSemestral;
import pe.nexosoftware.planilla360.planilla.domain.service.CalculadoraTiempoDeServicio;
import pe.nexosoftware.planilla360.planilla.domain.service.JornadaLaboral;

/**
 * Reúne los datos de otros módulos y le pide al dominio las líneas de una planilla. Cada tipo de planilla tiene su
 * propio armado: la mensual usa asistencia, horas extra y vacaciones; la gratificación y la CTS usan el tiempo de
 * servicio del semestre y lo ya pagado.
 */
@Component
class ArmadorDeLineas {

    private final PersonalDelPeriodo personalDelPeriodo;
    private final ConsultaPersonalPort personal;
    private final ConsultaCatalogoPort catalogo;
    private final ConsultaEmpresaPort empresa;
    private final ConsultaAsistenciaPort asistencia;
    private final ConsultaVacacionesPort vacaciones;
    private final HistorialPlanillaPort historial;
    private final CalculadoraLineaMensual calculadoraMensual = new CalculadoraLineaMensual();
    private final CalculadoraLineaSemestral calculadoraSemestral = new CalculadoraLineaSemestral();
    private final CalculadoraTiempoDeServicio tiempoDeServicio = new CalculadoraTiempoDeServicio();

    ArmadorDeLineas(
            PersonalDelPeriodo personalDelPeriodo,
            ConsultaPersonalPort personal,
            ConsultaCatalogoPort catalogo,
            ConsultaEmpresaPort empresa,
            ConsultaAsistenciaPort asistencia,
            ConsultaVacacionesPort vacaciones,
            HistorialPlanillaPort historial) {
        this.personalDelPeriodo = personalDelPeriodo;
        this.personal = personal;
        this.catalogo = catalogo;
        this.empresa = empresa;
        this.asistencia = asistencia;
        this.vacaciones = vacaciones;
        this.historial = historial;
    }

    /** Rango en que se busca al personal: todo el mes en la mensual; el último día del semestre en los beneficios. */
    record RangoDelPersonal(LocalDate desde, LocalDate hasta) {
    }

    static RangoDelPersonal rangoDelPersonal(Periodo periodo, TipoPlanilla tipo) {
        return switch (tipo) {
            case MENSUAL -> new RangoDelPersonal(periodo.inicio(), periodo.fin());
            case GRATIFICACION -> finDelSemestre(SemestreDeBeneficio.deGratificacion(periodo));
            case CTS -> finDelSemestre(SemestreDeBeneficio.deCts(periodo));
        };
    }

    private static RangoDelPersonal finDelSemestre(SemestreDeBeneficio semestre) {
        return new RangoDelPersonal(semestre.fin(), semestre.fin());
    }

    List<LineaPlanilla> armar(Periodo periodo, TipoPlanilla tipo) {
        return switch (tipo) {
            case MENSUAL -> armarMensual(periodo);
            case GRATIFICACION -> armarSemestral(periodo, SemestreDeBeneficio.deGratificacion(periodo), false);
            case CTS -> armarSemestral(periodo, SemestreDeBeneficio.deCts(periodo), true);
        };
    }

    private List<LineaPlanilla> armarMensual(Periodo periodo) {
        ParametrosLegales parametros = catalogo.parametrosEn(periodo.fin());
        SobretasasHorasExtra sobretasas = catalogo.sobretasasHorasExtraEn(periodo.fin());
        RegimenLaboral regimenLaboral = empresa.regimenLaboral();
        Map<UUID, NovedadesAsistencia> novedades = asistencia.novedadesDelMes(periodo);
        Map<UUID, List<BigDecimal>> horasExtra = asistencia.horasExtraAprobadasDelMes(periodo);
        Map<UUID, Integer> diasSinGoce = vacaciones.diasDePermisoSinGoce(periodo);
        Map<UUID, Dinero> percibidos = historial.ingresosPercibidosEnElAnio(periodo);
        Map<UUID, Integer> minutosDeHorario = asistencia.minutosDeHorarioEn(periodo.fin());

        List<LineaPlanilla> lineas = new ArrayList<>();
        for (TrabajadorConRegimen t : personalDelPeriodo.resolver(periodo.inicio(), periodo.fin()).conRegimen()) {
            UUID id = t.trabajador().id();
            NovedadesAsistencia delMes = novedades.getOrDefault(id, NovedadesAsistencia.NINGUNA);
            DatosMensualesTrabajador datos = new DatosMensualesTrabajador(
                    id,
                    t.contrato().sueldoBasico(),
                    JornadaLaboral.horasDiarias(minutosDeHorario.get(id), t.contrato().jornadaSemanal(),
                            t.contrato().diasLaborables()),
                    t.regimen(),
                    regimenLaboral,
                    personal.hijosDe(id),
                    diasLaborados(t, periodo),
                    delMes.minutosTardanza(),
                    delMes.faltasInjustificadas() + diasSinGoce.getOrDefault(id, 0),
                    horasExtra.getOrDefault(id, List.of()),
                    percibidos.getOrDefault(id, Dinero.CERO));
            lineas.add(calculadoraMensual.calcular(datos, periodo, parametros, sobretasas.primerasDosHoras(),
                    sobretasas.desdeLaTercera()));
        }
        return lineas;
    }

    private List<LineaPlanilla> armarSemestral(Periodo periodo, SemestreDeBeneficio semestre, boolean esCts) {
        ParametrosLegales parametros = catalogo.parametrosEn(periodo.fin());
        RegimenLaboral regimenLaboral = empresa.regimenLaboral();
        Periodo primerMes = Periodo.de(semestre.inicio());
        Periodo ultimoMes = Periodo.de(semestre.fin());
        Map<UUID, List<Dinero>> horasExtra = historial.horasExtraPagadas(primerMes, ultimoMes);
        Map<UUID, Dinero> ultimasGratificaciones = esCts ? historial.ultimaGratificacion(periodo) : Map.of();

        List<LineaPlanilla> lineas = new ArrayList<>();
        for (TrabajadorConRegimen t : personalDelPeriodo.resolver(semestre.fin(), semestre.fin()).conRegimen()) {
            UUID id = t.trabajador().id();
            TiempoComputable tiempo = tiempoDeServicio.computar(t.trabajador().ingreso(), t.trabajador().cese(),
                    semestre);
            if (!generaBeneficio(tiempo, esCts)) {
                continue;
            }
            DatosBeneficioSemestral datos = new DatosBeneficioSemestral(
                    id,
                    t.contrato().sueldoBasico(),
                    t.regimen(),
                    regimenLaboral,
                    personal.hijosDe(id),
                    horasExtra.getOrDefault(id, List.of()),
                    tiempo,
                    ultimasGratificaciones.getOrDefault(id, Dinero.CERO));
            lineas.add(esCts
                    ? calculadoraSemestral.cts(datos, periodo, parametros)
                    : calculadoraSemestral.gratificacion(datos, periodo, parametros));
        }
        return lineas;
    }

    /** La gratificación exige al menos un mes completo; la CTS se paga desde el primer día completo. */
    private static boolean generaBeneficio(TiempoComputable tiempo, boolean esCts) {
        return esCts ? tiempo.meses() > 0 || tiempo.dias() > 0 : tiempo.meses() > 0;
    }

    private static int diasLaborados(TrabajadorConRegimen t, Periodo periodo) {
        LocalDate desde = t.trabajador().ingreso().isAfter(periodo.inicio())
                ? t.trabajador().ingreso()
                : periodo.inicio();
        LocalDate hasta = t.trabajador().cese() != null && t.trabajador().cese().isBefore(periodo.fin())
                ? t.trabajador().cese()
                : periodo.fin();
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }
}
