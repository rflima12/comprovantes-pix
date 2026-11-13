package tech.ada.pix.gravador;

import tech.ada.pix.contracts.ComprovanteView;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A "porta da frente" do GRAVADOR (Aula 2). Ninguém escreve direto no banco dele:
 * quem quiser gravar, pede por aqui.
 *
 * POST: 201 → gravou agora · 200 → já estava gravado (idempotência).
 * Aula 5: o pedido de gravação também chega pela fila (ConsumidorDeGravacao), e o emissor passa
 * a usar a fila no exercício. Esta porta continua aqui para as demos (o gravar.json da Aula 4).
 *
 * GET (Aula 4): 200 → o comprovante · 404 → não está gravado. Quem chama é a consulta.
 *
 * Aula 6: gravou por aqui também é fato. Publica no Kafka, como o ConsumidorDeGravacao.
 */
@RestController
@RequestMapping("/interno/comprovantes")
public class GravadorController {

    private static final Logger log = LoggerFactory.getLogger(GravadorController.class);

    private final GravadorService gravador;
    private final PublicadorDoFato fato;
    private final long atrasoMs;
    private final long leituraLentaMs;

    public GravadorController(GravadorService gravador, PublicadorDoFato fato,
                              @Value("${demo.atraso-ms:0}") long atrasoMs,
                              @Value("${demo.leitura-lenta-ms:0}") long leituraLentaMs) {
        this.gravador = gravador;
        this.fato = fato;
        this.atrasoMs = atrasoMs;
        this.leituraLentaMs = leituraLentaMs;
    }

    @PostMapping
    public ResponseEntity<Void> gravar(@RequestBody GravarComprovanteCommand command) throws InterruptedException {
        boolean gravouAgora = gravador.gravar(command);
        log.info("Recebi por HTTP o pedido de gravação do comprovante {}: {}",
                command.identificadorComprovante(), gravouAgora ? "gravei" : "já estava gravado");
        if (gravouAgora) {
            fato.publicar(command); // Aula 6: gravou agora. Avisa quem quiser saber.
        }
        Thread.sleep(atrasoMs); // ⚠️ só para a demo
        return ResponseEntity.status(gravouAgora ? HttpStatus.CREATED : HttpStatus.OK).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprovanteView> buscar(@PathVariable UUID id) throws InterruptedException {
        log.info("Lendo o comprovante {} do banco", id);
        Thread.sleep(leituraLentaMs); // ⚠️ só para a Aula 4: finge um banco cheio, que demora para responder
        return gravador.buscar(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
