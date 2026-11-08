package pe.nexosoftware.planilla360.planilla.infrastructure.adapter;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.catalogo.CatalogoApi;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.application.model.SobretasasHorasExtra;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaCatalogoPort;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;
import pe.nexosoftware.planilla360.planilla.domain.regimen.AfpVigente;

/** Capa anticorrupción hacia {@code catalogo}: los parámetros legales se leen versionados por fecha de vigencia. */
@Component
class CatalogoModuloAdapter implements ConsultaCatalogoPort {

    private final CatalogoApi catalogo;

    CatalogoModuloAdapter(CatalogoApi catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public ParametrosLegales parametrosEn(LocalDate fecha) {
        return new ParametrosLegales(
                Dinero.de(catalogo.valorParametro("RMV", fecha)),
                Dinero.de(catalogo.valorParametro("UIT", fecha)),
                catalogo.valorParametro("ASIG_FAMILIAR_PCT", fecha),
                catalogo.valorParametro("ONP_PCT", fecha),
                catalogo.valorParametro("AFP_APORTE_PCT", fecha),
                catalogo.valorParametro("AFP_PRIMA_PCT", fecha),
                catalogo.valorParametro("ESSALUD_PCT", fecha),
                catalogo.valorParametro("BONIF_EXTRAORDINARIA_PCT", fecha));
    }

    @Override
    public SobretasasHorasExtra sobretasasHorasExtraEn(LocalDate fecha) {
        return new SobretasasHorasExtra(
                catalogo.valorParametro("HE_SOBRETASA_1_PCT", fecha),
                catalogo.valorParametro("HE_SOBRETASA_2_PCT", fecha));
    }

    @Override
    public Optional<AfpVigente> afp(UUID afpId) {
        return catalogo.afps().stream()
                .filter(a -> a.id().equals(afpId))
                .findFirst()
                .map(a -> new AfpVigente(a.nombre(), a.comisionFlujo(), a.comisionMixta()));
    }

    @Override
    public boolean esFeriado(LocalDate fecha) {
        return catalogo.esFeriado(fecha);
    }
}
