package tech.ada.pix.emissor.gravacao;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tech.ada.pix.contracts.ComprovanteRequest;
import tech.ada.pix.contracts.FilaDeGravacao;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import tech.ada.pix.contracts.TipoChavePix;
import tech.ada.pix.contracts.TipoDocumento;
import tech.ada.pix.emissor.dominio.ChavePix;
import tech.ada.pix.emissor.dominio.Comprovante;
import tech.ada.pix.emissor.dominio.ContaBancaria;
import tech.ada.pix.emissor.dominio.Dinheiro;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Exercício da Aula 5, passo 2: o adapter publica o pedido de gravação na fila, em vez de chamar
 * o gravador por HTTP. O RabbitTemplate é de mentira (Mockito): o teste não precisa do RabbitMQ.
 */
class GravacaoAdapterTest {

    static final ComprovanteRequest PIX = new ComprovanteRequest(
            "Giovanni", TipoDocumento.CPF, "50329291076", "2022", "00276", "0",
            new BigDecimal("23.99"), TipoChavePix.CELULAR, "11948755536",
            "Fernando", "churrasco", LocalDateTime.parse("2022-04-10T20:03:57"));

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final GravacaoAdapter adapter = new GravacaoAdapter(rabbit);

    static Comprovante comprovante() {
        return Comprovante.emitir("Giovanni", new ContaBancaria("2022", "00276", "0"),
                new Dinheiro(new BigDecimal("23.99")), new ChavePix(TipoChavePix.CELULAR, "11948755536"));
    }

    @Test
    void publicaOPedidoNaExchangeComAEtiquetaDaGravacao() {
        Comprovante comprovante = comprovante();

        adapter.gravar(comprovante, PIX);

        // No balcão (a exchange), com a etiqueta (a routing key). Nunca direto na fila.
        verify(rabbit).convertAndSend(FilaDeGravacao.EXCHANGE, FilaDeGravacao.ROUTING_KEY,
                new GravarComprovanteCommand(comprovante.id(), PIX));
    }

    @Test
    void comORabbitMqForaDoArAGravacaoFalhaEASagaCompensa() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        // É a mesma exceção de antes: o EmissaoService compensa (INVALIDADO) e o cliente recebe 503.
        assertThatThrownBy(() -> adapter.gravar(comprovante(), PIX))
                .isInstanceOf(GravacaoFalhouException.class);
    }
}
