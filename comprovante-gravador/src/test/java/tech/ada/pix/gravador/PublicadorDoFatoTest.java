package tech.ada.pix.gravador;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.ArgumentCaptor;
import tech.ada.pix.contracts.ComprovanteGravadoEvent;
import tech.ada.pix.contracts.ComprovanteRequest;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import tech.ada.pix.contracts.TipoChavePix;
import tech.ada.pix.contracts.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aula 6: o gravador publica o fato no tópico certo, com o id do comprovante como CHAVE.
 * O KafkaTemplate é de mentira (Mockito): o teste não precisa do Kafka rodando.
 */
class PublicadorDoFatoTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, ComprovanteGravadoEvent> kafka = mock(KafkaTemplate.class);
    private final PublicadorDoFato publicador = new PublicadorDoFato(kafka);

    @Test
    void publicaNoTopicoComOIdDoComprovanteComoChaveEOValorJunto() {
        when(kafka.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
        UUID id = UUID.randomUUID();

        publicador.publicar(new GravarComprovanteCommand(id, new ComprovanteRequest(
                "Giovanni", TipoDocumento.CPF, "50329291076", "2022", "00276", "0",
                new BigDecimal("23.99"), TipoChavePix.CELULAR, "11948755536",
                "Fernando", "churrasco", LocalDateTime.parse("2022-04-10T20:03:57"))));

        // Mesma chave, mesma partição: tudo de um comprovante fica em ordem.
        ArgumentCaptor<ComprovanteGravadoEvent> fato = ArgumentCaptor.forClass(ComprovanteGravadoEvent.class);
        verify(kafka).send(eq("comprovante-gravado"), eq(id.toString()), fato.capture());
        assertThat(fato.getValue().identificadorComprovante()).isEqualTo(id);
        assertThat(fato.getValue().valorTransacao()).isEqualByComparingTo("23.99");   // o valor vai junto
    }
}
