package tech.ada.pix.emissor.gravacao;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tech.ada.pix.contracts.ComprovanteRequest;
import tech.ada.pix.contracts.FilaDeGravacao;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import tech.ada.pix.emissor.dominio.Comprovante;

/**
 * A "tomada" entre a emissão e a gravação.
 *
 * O EmissaoService só sabe "gravar"; não sabe o que existe do outro lado.
 * Aula 5: em vez de chamar o gravador por HTTP e esperar, este adapter publica o pedido de
 * gravação no RabbitMQ e volta na hora. O EmissaoService nem percebeu a troca.
 */
@Component
public class GravacaoAdapter {

    private final RabbitTemplate rabbit;

    public GravacaoAdapter(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void gravar(Comprovante comprovante, ComprovanteRequest dados) {
        try {
            // Entrega no balcão (a exchange), com a etiqueta (a routing key).
            // Quem escolhe a fila é o RabbitMQ, pelo binding.
            rabbit.convertAndSend(FilaDeGravacao.EXCHANGE, FilaDeGravacao.ROUTING_KEY,
                    new GravarComprovanteCommand(comprovante.id(), dados));
        } catch (AmqpException falha) {
            // O RabbitMQ está fora do ar: a SAGA compensa, como antes (503 e INVALIDADO).
            throw new GravacaoFalhouException(comprovante.id(), falha);
        }
    }
}
