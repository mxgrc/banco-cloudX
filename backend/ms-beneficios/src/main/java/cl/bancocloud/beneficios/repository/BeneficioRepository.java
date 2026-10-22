package cl.bancocloud.beneficios.repository;

import cl.bancocloud.beneficios.domain.Beneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface BeneficioRepository extends JpaRepository<Beneficio, UUID> {
    Page<Beneficio> findByDisponibleTrue(Pageable pageable);
}
