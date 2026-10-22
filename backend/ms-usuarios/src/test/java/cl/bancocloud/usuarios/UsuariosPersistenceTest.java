package cl.bancocloud.usuarios;

import cl.bancocloud.usuarios.domain.*;
import cl.bancocloud.usuarios.repository.UsuarioPortalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
class UsuariosPersistenceTest {
    private final Instant fecha = Instant.parse("2026-09-12T12:00:00Z");
    @Autowired UsuarioPortalRepository usuarios;
    @Autowired TestEntityManager em;

    @Test void nuevoUsuarioPermanecePendiente() {
        usuarios.saveAndFlush(new UsuarioPortal("cliente-sub", fecha));
        em.clear();
        UsuarioPortal usuario = usuarios.findById("cliente-sub").orElseThrow();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE);
        assertThat(usuario.isActivo()).isFalse();
    }
    @Test void aprobarDesactivarReactivarConAuditoria() {
        UsuarioPortal usuario = usuarios.saveAndFlush(new UsuarioPortal("cliente-sub", fecha));
        usuario.aprobar("admin-sub", fecha.plusSeconds(1));
        assertThat(usuario.isActivo()).isTrue();
        usuario.desactivar("admin-sub", fecha.plusSeconds(2));
        assertThat(usuario.isActivo()).isFalse();
        usuario.reactivar("otro-admin-sub", fecha.plusSeconds(3));
        usuarios.flush(); em.clear();
        UsuarioPortal obtenido = usuarios.findById("cliente-sub").orElseThrow();
        assertThat(obtenido.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(obtenido.getModificadoPorSub()).isEqualTo("otro-admin-sub");
        assertThat(obtenido.getFechaCambioEstado()).isEqualTo(fecha.plusSeconds(3));
    }
    @Test void noPermiteReactivarSinAprobacionPrevia() {
        UsuarioPortal usuario = new UsuarioPortal("cliente-sub", fecha);
        assertThatThrownBy(() -> usuario.reactivar("admin", fecha)).isInstanceOf(IllegalStateException.class);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE);
    }
    @Test void noPermiteAprobarDosVeces() {
        UsuarioPortal usuario = new UsuarioPortal("cliente-sub", fecha);
        usuario.aprobar("admin", fecha);
        assertThatThrownBy(() -> usuario.aprobar("admin", fecha)).isInstanceOf(IllegalStateException.class);
    }
    @Test void cambioInvalidoNoMutaElUsuario() {
        UsuarioPortal usuario = new UsuarioPortal("cliente-sub", fecha);
        assertThatThrownBy(() -> usuario.aprobar(" ", fecha)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuario.aprobar("admin", fecha.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE);
        assertThat(usuario.getFechaCambioEstado()).isNull();
    }
    @Test void listaPendientesSinMezclarActivos() {
        usuarios.save(new UsuarioPortal("pendiente", fecha));
        UsuarioPortal activo = new UsuarioPortal("activo", fecha);
        activo.aprobar("admin", fecha);
        usuarios.saveAndFlush(activo);
        assertThat(usuarios.findByEstado(EstadoUsuario.PENDIENTE, PageRequest.of(0, 10)).getContent())
            .extracting(UsuarioPortal::getUsuarioSub).containsExactly("pendiente");
    }
}
