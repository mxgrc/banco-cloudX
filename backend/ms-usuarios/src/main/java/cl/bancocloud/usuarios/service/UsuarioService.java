package cl.bancocloud.usuarios.service;

import java.time.Instant;
import cl.bancocloud.usuarios.api.*;
import cl.bancocloud.usuarios.domain.*;
import cl.bancocloud.usuarios.repository.UsuarioPortalRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioPortalRepository repository;

    public UsuarioService(UsuarioPortalRepository repository) {
        this.repository = repository;
    }

    public UsuarioResponse registrar(String sub) {
        // Repetir el registro no reinicia ni reactiva una cuenta existente.
        return UsuarioResponse.from(repository.findById(sub).orElseGet(
                () -> repository.saveAndFlush(new UsuarioPortal(sub, Instant.now()))));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultar(String sub) {
        return UsuarioResponse.from(buscar(sub));
    }

    @Transactional(readOnly = true)
    public PaginaUsuarios listar(EstadoUsuario estado, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) {
            throw new IllegalArgumentException("Página >= 0 y tamaño entre 1 y 100");
        }
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("fechaRegistro").and(Sort.by("usuarioSub")));
        return PaginaUsuarios.from(estado == null ? repository.findAll(pageable) : repository.findByEstado(estado, pageable));
    }

    public UsuarioResponse aprobar(String sub, String actor) {
        UsuarioPortal usuario = buscar(sub);
        usuario.aprobar(actor, Instant.now());
        return guardar(usuario);
    }

    public UsuarioResponse desactivar(String sub, String actor) {
        UsuarioPortal usuario = buscar(sub);
        usuario.desactivar(actor, Instant.now());
        return guardar(usuario);
    }

    public UsuarioResponse reactivar(String sub, String actor) {
        UsuarioPortal usuario = buscar(sub);
        usuario.reactivar(actor, Instant.now());
        return guardar(usuario);
    }

    private UsuarioResponse guardar(UsuarioPortal usuario) {
        repository.flush();
        return UsuarioResponse.from(usuario);
    }

    private UsuarioPortal buscar(String sub) {
        return repository.findById(sub).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no registrado en el portal"));
    }
}
