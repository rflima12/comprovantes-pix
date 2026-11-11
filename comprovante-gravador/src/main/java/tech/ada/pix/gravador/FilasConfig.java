package tech.ada.pix.gravador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.ada.pix.contracts.FilaDeGravacao;

/**
 * Aula 5: o caminho da mensagem de gravação no RabbitMQ.
 *
 * <pre>
 *   gravacao.ex ──"gravacao"──► gravacao.q ──► ConsumidorDeGravacao
 *                                   │ falhou de vez
 *                                   ▼
 *   gravacao.dlx ──"gravacao"──► gravacao.dlq   (alguém olha)
 * </pre>
 *
 * O gravador e o emissor declaram AS MESMAS peças, com os mesmos argumentos. Assim, não importa
 * quem sobe primeiro: o caminho existe antes da primeira mensagem, e nada se perde no meio.
 * Se um lado declarar diferente, o RabbitMQ recusa (PRECONDITION_FAILED): mudou aqui, muda lá
 * também (comprovante-emissor, gravacao/FilasConfig.java).
 *
 * O Spring cria as peças no RabbitMQ sozinho, na primeira conexão. Se já existem, só confere.
 */
@Configuration
public class FilasConfig {

    /** O balcão: o emissor publica aqui, e a exchange manda para a fila certa pela etiqueta. */
    @Bean
    DirectExchange exchangeDeGravacao() {
        return new DirectExchange(FilaDeGravacao.EXCHANGE);
    }

    /**
     * O trilho: onde o pedido de gravação espera o gravador. "durable": a fila sobrevive a um
     * reinício do RabbitMQ. E já nasce ligada à DLX: mensagem rejeitada de vez vai para lá.
     */
    @Bean
    Queue filaDeGravacao() {
        return QueueBuilder.durable(FilaDeGravacao.FILA)
                .deadLetterExchange(FilaDeGravacao.DLX)
                .build();
    }

    /** A plaquinha: mensagem com a etiqueta "gravacao" vai para a fila gravacao.q. */
    @Bean
    Binding ligacaoDaFila() {
        return BindingBuilder.bind(filaDeGravacao()).to(exchangeDeGravacao()).with(FilaDeGravacao.ROUTING_KEY);
    }

    /** A exchange da DLQ: recebe o que a gravacao.q rejeitou de vez. */
    @Bean
    DirectExchange exchangeDaDlq() {
        return new DirectExchange(FilaDeGravacao.DLX);
    }

    /** A caixinha do gerente: as mensagens que falharam de vez ficam aqui, para alguém olhar. */
    @Bean
    Queue dlq() {
        return QueueBuilder.durable(FilaDeGravacao.DLQ).build();
    }

    /** A mensagem rejeitada chega na DLX com a mesma etiqueta ("gravacao") e vai para a DLQ. */
    @Bean
    Binding ligacaoDaDlq() {
        return BindingBuilder.bind(dlq()).to(exchangeDaDlq()).with(FilaDeGravacao.ROUTING_KEY);
    }

    /** As mensagens vão em JSON, com o mesmo Jackson do serviço (snake_case), igual ao HTTP. */
    @Bean
    MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
