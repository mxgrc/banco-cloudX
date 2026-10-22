package cl.bancocloud.beneficios.repository;

import cl.bancocloud.beneficios.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> { }
