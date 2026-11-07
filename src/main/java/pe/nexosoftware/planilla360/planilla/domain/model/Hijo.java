package pe.nexosoftware.planilla360.planilla.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/** Hijo del trabajador: solo importa su edad y si estudia, para la asignación familiar. */
public record Hijo(LocalDate nacimiento, boolean estudia) {

    public Hijo {
        Objects.requireNonNull(nacimiento, "La fecha de nacimiento es obligatoria");
    }
}
