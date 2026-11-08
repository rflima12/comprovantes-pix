package tech.ada.pix.consulta;

import tech.ada.pix.contracts.ComprovanteView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * A consulta de um comprovante (Aula 4).
 *
 * Hoje, toda consulta vai ao gravador, que é a fonte da verdade. Como o comprovante não muda
 * depois de gravado, isso é trabalho repetido: é aqui que o cache entra, no exercício.
 */
@Service
public class ConsultaService {

    private static final Logger log = LoggerFactory.getLogger(ConsultaService.class);

    private final GravadorClient gravador;

    public ConsultaService(GravadorClient gravador) {
        this.gravador = gravador;
    }

    /** Vazio se o gravador não tem esse comprovante (o controller responde 404). */
    public Optional<ComprovanteView> buscar(UUID id) {
        log.info("Indo ao gravador buscar o comprovante {}", id);
        return gravador.buscar(id);
    }
}
