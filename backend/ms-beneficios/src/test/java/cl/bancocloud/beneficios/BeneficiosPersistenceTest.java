package cl.bancocloud.beneficios;

import cl.bancocloud.beneficios.domain.*;
import cl.bancocloud.beneficios.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
class BeneficiosPersistenceTest {
    @Autowired CategoriaRepository categorias;
    @Autowired BeneficioRepository beneficios;
    @Autowired TestEntityManager em;

    @Test void persisteCategoriaYDescuentoExacto() {
        Categoria categoria = categorias.saveAndFlush(new Categoria(" Gastronomía "));
        Beneficio beneficio = new Beneficio("Restaurante", "Descuento al pagar", new BigDecimal("12.50"));
        beneficio.asignarCategoria(categoria);
        beneficios.saveAndFlush(beneficio);
        em.clear();
        Beneficio obtenido = beneficios.findById(beneficio.getId()).orElseThrow();
        assertThat(obtenido.getCategoria().getNombre()).isEqualTo("Gastronomía");
        assertThat(obtenido.getDescuento()).isEqualByComparingTo("12.50");
    }
    @Test void separaDisponiblesDelListadoCompleto() {
        beneficios.save(new Beneficio("Activo", "Disponible", new BigDecimal("20")));
        Beneficio noDisponible = new Beneficio("Pausado", "No disponible", new BigDecimal("10"));
        noDisponible.cambiarDisponibilidad(false);
        beneficios.saveAndFlush(noDisponible);
        assertThat(beneficios.count()).isEqualTo(2);
        assertThat(beneficios.findByDisponibleTrue(PageRequest.of(0, 10)).getContent())
            .extracting(Beneficio::getNombre).containsExactly("Activo");
    }
    @Test void permiteCrearYAsignarCategoriaEnOperacionesSeparadas() {
        Beneficio beneficio = beneficios.saveAndFlush(new Beneficio("Nuevo", "Sin categoría inicial", BigDecimal.TEN));
        assertThat(beneficio.getCategoria()).isNull();
        beneficio.asignarCategoria(categorias.save(new Categoria("Viajes")));
        beneficios.flush();
        em.clear();
        assertThat(beneficios.findById(beneficio.getId()).orElseThrow().getCategoria().getNombre()).isEqualTo("Viajes");
    }
    @Test void rechazaCategoriaDuplicadaEnBaseDeDatos() {
        categorias.saveAndFlush(new Categoria("Viajes"));
        assertThatThrownBy(() -> categorias.saveAndFlush(new Categoria("Viajes")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "100.01", "0.001"})
    void rechazaDescuentosInvalidos(String valor) {
        assertThatThrownBy(() -> new Beneficio("Nombre", "Descripción", new BigDecimal(valor)))
            .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void aceptaCienPorCiento() {
        assertThat(new Beneficio("Nombre", "Descripción", new BigDecimal("100")).getDescuento()).isEqualByComparingTo("100");
    }
    @Test void rechazaNombreVacio() {
        assertThatThrownBy(() -> new Beneficio(" ", "Descripción", BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
    }
}
