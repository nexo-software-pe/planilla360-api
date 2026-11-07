package pe.nexosoftware.planilla360.identidad.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.nexosoftware.planilla360.identidad.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revocado = true where t.usuarioId = :usuarioId and t.revocado = false")
    int revocarTodosDelUsuario(@Param("usuarioId") UUID usuarioId);
}
