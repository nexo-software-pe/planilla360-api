package pe.nexosoftware.planilla360.catalogo.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import pe.nexosoftware.planilla360.catalogo.AfpResumen;
import pe.nexosoftware.planilla360.catalogo.dto.AfpResponse;
import pe.nexosoftware.planilla360.catalogo.dto.AreaResponse;
import pe.nexosoftware.planilla360.catalogo.dto.CargoResponse;
import pe.nexosoftware.planilla360.catalogo.dto.FeriadoResponse;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalResponse;
import pe.nexosoftware.planilla360.catalogo.entity.Afp;
import pe.nexosoftware.planilla360.catalogo.entity.Area;
import pe.nexosoftware.planilla360.catalogo.entity.Cargo;
import pe.nexosoftware.planilla360.catalogo.entity.Feriado;
import pe.nexosoftware.planilla360.catalogo.entity.ParametroLegal;

@Mapper(componentModel = "spring")
public interface CatalogoMapper {

    AreaResponse aResponse(Area area);

    CargoResponse aResponse(Cargo cargo);

    AfpResponse aResponse(Afp afp);

    ParametroLegalResponse aResponse(ParametroLegal parametro);

    FeriadoResponse aResponse(Feriado feriado);

    List<AfpResponse> aAfpResponses(List<Afp> afps);

    List<ParametroLegalResponse> aParametroResponses(List<ParametroLegal> parametros);

    AfpResumen aResumen(AfpResponse afp);
}
