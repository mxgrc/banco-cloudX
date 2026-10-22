package cl.bancocloud.inscripciones.repository;

import cl.bancocloud.inscripciones.domain.Inscripcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    Page<Inscripcion> findByUsuarioSub(String usuarioSub, Pageable pageable);
    boolean existsByUsuarioSubAndBeneficioId(String usuarioSub, UUID beneficioId);
}
