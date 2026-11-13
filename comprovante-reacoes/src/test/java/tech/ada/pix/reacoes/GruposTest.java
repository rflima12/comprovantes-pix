package tech.ada.pix.reacoes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aula 6: cada reação lê o tópico no seu próprio consumer group.
 *
 * Não precisa do Kafka rodando: o serviço sobe sem criar o tópico e sem começar a escutar,
 * e o teste confere quem está escutando, e em que grupo.
 */
@SpringBootTest(properties = {
        "spring.kafka.admin.auto-create=false",     // não cria o tópico no Kafka de verdade
        "spring.kafka.listener.auto-startup=false"  // não escuta o Kafka de verdade
})
class GruposTest {

    @Autowired
    private KafkaListenerEndpointRegistry ouvintes;

    @Test
    void oSmsLeOTopicoNoGrupoNotificacao() {
        assertThat(ouvintes.getListenerContainers())
                .extracting(MessageListenerContainer::getGroupId)
                .containsExactly("notificacao");
        assertThat(ouvintes.getListenerContainers())
                .allSatisfy(ouvinte -> assertThat(ouvinte.getContainerProperties().getTopics())
                        .containsExactly("comprovante-gravado"));
    }
}
