package pe.nexosoftware.planilla360.catalogo.dto;

import java.time.LocalDate;
import java.util.UUID;

public record FeriadoResponse(UUID id, LocalDate fecha, String descripcion, String ambito) {
}
