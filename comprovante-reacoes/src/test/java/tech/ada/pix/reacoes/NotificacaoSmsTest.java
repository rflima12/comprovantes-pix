package tech.ada.pix.reacoes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tech.ada.pix.contracts.ComprovanteGravadoEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aula 6: o SMS lê o fato e avisa o cliente. Não precisa do Kafka: o teste chama o método direto,
 * como o Spring faz a cada mensagem que chega, e confere o log.
 */
@ExtendWith(OutputCaptureExtension.class)
class NotificacaoSmsTest {

    @Test
    void avisaOClienteComOValorQueVeioNoFato(CapturedOutput log) {
        UUID id = UUID.randomUUID();
        var fato = new ComprovanteGravadoEvent(id, new BigDecimal("23.99"), LocalDateTime.now());

        new NotificacaoSms().quandoGravado(fato, 1, 0);

        assertThat(log).contains("SMS enviado: seu PIX de R$ 23.99 foi concluído (comprovante " + id
                + ") [partição 1, offset 0]");
    }
}
