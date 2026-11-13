package tech.ada.pix.gravador;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import tech.ada.pix.contracts.TopicoComprovanteGravado;

/**
 * Aula 6: o tópico do fato "comprovante gravado", no Kafka.
 *
 * O gravador e o comprovante-reacoes declaram O MESMO tópico, como a topologia do RabbitMQ na
 * Aula 5: não importa quem sobe primeiro. O Spring cria o tópico no Kafka quando o serviço sobe.
 * Se já existe, só confere. Mudou aqui, muda lá também (comprovante-reacoes, TopicoConfig.java).
 */
@Configuration
public class TopicoConfig {

    @Bean
    NewTopic comprovanteGravado() {
        return TopicBuilder.name(TopicoComprovanteGravado.NOME)
                .partitions(TopicoComprovanteGravado.PARTICOES)
                .replicas(1)   // no Docker, o Kafka tem uma máquina só (um broker)
                .build();
    }
}
