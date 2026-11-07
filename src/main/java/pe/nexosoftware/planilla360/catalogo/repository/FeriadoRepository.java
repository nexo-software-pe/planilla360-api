package pe.nexosoftware.planilla360.catalogo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nexosoftware.planilla360.catalogo.entity.Feriado;

public interface FeriadoRepository extends JpaRepository<Feriado, UUID> {

    List<Feriado> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
