package tech.ada.pix.emissor.gravacao;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aula 5: o caminho da mensagem. Se este teste mudar, o do gravador (FilasConfigTest) muda junto:
 * os dois declaram as mesmas peças, ou o RabbitMQ recusa (PRECONDITION_FAILED).
 */
class FilasConfigTest {

    private final FilasConfig config = new FilasConfig();

    @Test
    void aFilaDeGravacaoNasceLigadaADlq() {
        Queue fila = config.filaDeGravacao();

        assertThat(fila.getName()).isEqualTo("gravacao.q");
        assertThat(fila.isDurable()).isTrue();
        assertThat(fila.getArguments()).containsEntry("x-dead-letter-exchange", "gravacao.dlx");
        assertThat(config.ligacaoDaFila().getExchange()).isEqualTo("gravacao.ex");
        assertThat(config.ligacaoDaFila().getRoutingKey()).isEqualTo("gravacao");
        assertThat(config.ligacaoDaDlq().getExchange()).isEqualTo("gravacao.dlx");
        assertThat(config.ligacaoDaDlq().getDestination()).isEqualTo("gravacao.dlq");
    }
}
