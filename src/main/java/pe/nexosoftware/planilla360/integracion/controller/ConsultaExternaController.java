package pe.nexosoftware.planilla360.integracion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.integracion.model.ContribuyenteRuc;
import pe.nexosoftware.planilla360.integracion.model.PersonaDni;
import pe.nexosoftware.planilla360.integracion.model.TipoCambio;
import pe.nexosoftware.planilla360.integracion.service.ConsultaExternaService;

@RestController
@RequestMapping("/api/v1/consultas")
@PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Consultas externas",
        description = "DNI, RUC y tipo de cambio desde un servicio externo (con reintentos y cortacircuito)")
public class ConsultaExternaController {

    private final ConsultaExternaService consultas;

    public ConsultaExternaController(ConsultaExternaService consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Consulta un DNI")
    public PersonaDni dni(@PathVariable String dni) {
        return consultas.dni(dni);
    }

    @GetMapping("/ruc/{ruc}")
    @Operation(summary = "Consulta un RUC")
    public ContribuyenteRuc ruc(@PathVariable String ruc) {
        return consultas.ruc(ruc);
    }

    @GetMapping("/tipo-cambio")
    @Operation(summary = "Tipo de cambio del día (USD)")
    public TipoCambio tipoCambio() {
        return consultas.tipoCambio();
    }
}
