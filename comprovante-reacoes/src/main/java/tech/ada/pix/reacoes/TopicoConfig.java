package tech.ada.pix.reacoes;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import tech.ada.pix.contracts.TopicoComprovanteGravado;


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
