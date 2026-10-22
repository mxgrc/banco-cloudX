package cl.bancocloud.usuarios.repository;

import cl.bancocloud.usuarios.domain.EstadoUsuario;
import cl.bancocloud.usuarios.domain.UsuarioPortal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioPortalRepository extends JpaRepository<UsuarioPortal, String> {
    Page<UsuarioPortal> findByEstado(EstadoUsuario estado, Pageable pageable);
}
