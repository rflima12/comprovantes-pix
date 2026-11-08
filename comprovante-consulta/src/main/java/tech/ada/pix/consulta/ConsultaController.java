package tech.ada.pix.consulta;

import tech.ada.pix.contracts.ComprovanteView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Entrypoint do bounded context CONSULTA (Aula 4).
 *
 * 200 → o comprovante · 404 → não está gravado.
 */
@RestController
@RequestMapping("/comprovantes")
public class ConsultaController {

    private static final Logger log = LoggerFactory.getLogger(ConsultaController.class);

    private final ConsultaService service;

    public ConsultaController(ConsultaService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprovanteView> consultar(@PathVariable UUID id) {
        log.info("Pediram o comprovante {}", id);
        return service.buscar(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
