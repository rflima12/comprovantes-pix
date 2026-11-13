package tech.ada.pix.gravador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercício da Aula 5, passo 5: o retry com backoff, ligado no application.yml do gravador.
 *
 * Não precisa do RabbitMQ: confere a configuração que o Spring leu e que o consumidor da fila usa.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",   // não escuta o RabbitMQ de verdade
        "spring.kafka.admin.auto-create=false",                 // não cria o tópico no Kafka de verdade
        "spring.datasource.url=jdbc:h2:mem:gravador-consumidor;DB_CLOSE_DELAY=-1"
})
class RetryComBackoffTest {

    @Autowired
    private RabbitProperties rabbit;

    @Autowired
    private SimpleRabbitListenerContainerFactory fabricaDeConsumidores;

    @MockBean
    private PublicadorDoFato fato;   // o Kafka de mentira: o teste não precisa do Kafka rodando

    @Test
    void tentaTresVezesEsperandoCadaVezMaisAntesDaDlq() {
        RabbitProperties.ListenerRetry retry = rabbit.getListener().getSimple().getRetry();

        assertThat(retry.isEnabled()).isTrue();
        assertThat(retry.getMaxAttempts()).isEqualTo(3);                        // a primeira e mais duas
        assertThat(retry.getInitialInterval()).isEqualTo(Duration.ofSeconds(1)); // 1 s antes da 2ª...
        assertThat(retry.getMultiplier()).isEqualTo(2.0);                       // ...e 2 s antes da 3ª
    }

    @Test
    void oConsumidorDaFilaUsaORetry() {
        // Com o retry ligado, o Spring põe o "tenta de novo" em volta de cada entrega.
        assertThat(fabricaDeConsumidores.getAdviceChain()).isNotEmpty();
    }
}
