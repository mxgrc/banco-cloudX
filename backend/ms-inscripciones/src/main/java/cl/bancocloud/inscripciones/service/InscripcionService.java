package cl.bancocloud.inscripciones.service;
import java.time.Instant;
import java.util.UUID;
import cl.bancocloud.inscripciones.api.*;
import cl.bancocloud.inscripciones.domain.Inscripcion;
import cl.bancocloud.inscripciones.repository.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
@Transactional
public class InscripcionService {
    private final InscripcionRepository repository;
    public InscripcionService(InscripcionRepository repository) { this.repository=repository; }
    public InscripcionDto crear(String sub, UUID id) {
        if(repository.existsByUsuarioSubAndBeneficioId(sub,id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El cliente ya está inscrito");
        return InscripcionDto.from(repository.saveAndFlush(new Inscripcion(sub,id,Instant.now())));
    }
    @Transactional(readOnly=true)
    public Pagina<InscripcionDto> propias(String sub,int pagina,int tamano) {
        return Pagina.from(repository.findByUsuarioSub(sub,Pagina.request(pagina,tamano)).map(InscripcionDto::from));
    }
}
