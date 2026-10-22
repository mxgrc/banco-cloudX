package cl.bancocloud.beneficios.service;
import java.util.UUID;
import cl.bancocloud.beneficios.api.*;
import cl.bancocloud.beneficios.api.BeneficioDto.*;
import cl.bancocloud.beneficios.domain.*;
import cl.bancocloud.beneficios.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BeneficioService {
    private final BeneficioRepository beneficios;
    private final CategoriaRepository categorias;
    public BeneficioService(BeneficioRepository beneficios, CategoriaRepository categorias) {
        this.beneficios = beneficios; this.categorias = categorias;
    }
    public BeneficioDto crear(Crear dto) {
        return BeneficioDto.from(beneficios.saveAndFlush(new Beneficio(dto.nombre(), dto.descripcion(), dto.descuento())));
    }
    @Transactional(readOnly=true)
    public Pagina<BeneficioDto> listar(boolean disponibles, int pagina, int tamano) {
        var page = Pagina.request(pagina, tamano);
        return Pagina.from((disponibles ? beneficios.findByDisponibleTrue(page) : beneficios.findAll(page)).map(BeneficioDto::from));
    }
    @Transactional(readOnly=true)
    public BeneficioDto disponible(UUID id) {
        Beneficio b = buscar(id);
        if (!b.isDisponible()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Beneficio no disponible para inscripción");
        return BeneficioDto.from(b);
    }
    public BeneficioDto asignar(UUID id, UUID categoriaId) {
        Beneficio b = buscar(id);
        b.asignarCategoria(categorias.findById(categoriaId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada")));
        beneficios.flush();
        return BeneficioDto.from(b);
    }
    public BeneficioDto disponibilidad(UUID id, boolean disponible) {
        Beneficio b = buscar(id); b.cambiarDisponibilidad(disponible); beneficios.flush();
        return BeneficioDto.from(b);
    }
    public CategoriaDto crearCategoria(CrearCategoria dto) {
        Categoria c = categorias.saveAndFlush(new Categoria(dto.nombre()));
        return new CategoriaDto(c.getId(), c.getNombre());
    }
    @Transactional(readOnly=true)
    public Pagina<CategoriaDto> categorias(int pagina, int tamano) {
        return Pagina.from(categorias.findAll(Pagina.request(pagina, tamano)).map(c -> new CategoriaDto(c.getId(), c.getNombre())));
    }
    private Beneficio buscar(UUID id) {
        return beneficios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficio no encontrado"));
    }
}
