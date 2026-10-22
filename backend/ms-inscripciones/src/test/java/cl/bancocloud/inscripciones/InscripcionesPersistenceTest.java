package cl.bancocloud.inscripciones;

import cl.bancocloud.inscripciones.domain.Inscripcion;
import cl.bancocloud.inscripciones.repository.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
class InscripcionesPersistenceTest {
    @Autowired InscripcionRepository inscripciones;
    @Autowired TestEntityManager em;
    private final Instant fecha = Instant.parse("2026-09-12T12:00:00Z");

    @Test void persisteReferenciasLogicasSinTablasExternas() {
        UUID beneficio = UUID.randomUUID();
        Inscripcion inscripcion = inscripciones.saveAndFlush(new Inscripcion("cliente-sub", beneficio, fecha));
        em.clear();
        Inscripcion obtenida = inscripciones.findById(inscripcion.getId()).orElseThrow();
        assertThat(obtenida.getBeneficioId()).isEqualTo(beneficio);
        assertThat(obtenida.getFechaInscripcion()).isEqualTo(fecha);
    }
    @Test void consultaSoloLasInscripcionesDelSujeto() {
        inscripciones.save(new Inscripcion("cliente-a", UUID.randomUUID(), fecha));
        inscripciones.saveAndFlush(new Inscripcion("cliente-b", UUID.randomUUID(), fecha));
        assertThat(inscripciones.findByUsuarioSub("cliente-a", PageRequest.of(0, 10)).getContent())
            .extracting(Inscripcion::getUsuarioSub).containsExactly("cliente-a");
    }
    @Test void duplicadoEsRechazadoPorLaBaseNoSoloPorUnIf() {
        UUID beneficio = UUID.randomUUID();
        inscripciones.saveAndFlush(new Inscripcion("cliente-sub", beneficio, fecha));
        assertThat(inscripciones.existsByUsuarioSubAndBeneficioId("cliente-sub", beneficio)).isTrue();
        assertThatThrownBy(() -> inscripciones.saveAndFlush(new Inscripcion("cliente-sub", beneficio, fecha)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void dosClientesPuedenInscribirseEnElMismoBeneficio() {
        UUID beneficio = UUID.randomUUID();
        inscripciones.save(new Inscripcion("cliente-a", beneficio, fecha));
        inscripciones.saveAndFlush(new Inscripcion("cliente-b", beneficio, fecha));
        assertThat(inscripciones.count()).isEqualTo(2);
    }
    @Test void rechazaInscripcionSinIdentidad() {
        assertThatThrownBy(() -> new Inscripcion(" ", UUID.randomUUID(), fecha)).isInstanceOf(IllegalArgumentException.class);
    }
}
