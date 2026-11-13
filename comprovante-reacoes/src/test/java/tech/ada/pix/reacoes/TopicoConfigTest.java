package tech.ada.pix.reacoes;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aula 6: o tópico. Se este teste mudar, o do gravador (TopicoConfigTest) muda junto:
 * os dois declaram o mesmo tópico.
 */
class TopicoConfigTest {

    @Test
    void oTopicoTemTresParticoesEUmaReplica() {
        NewTopic topico = new TopicoConfig().comprovanteGravado();

        assertThat(topico.name()).isEqualTo("comprovante-gravado");
        assertThat(topico.numPartitions()).isEqualTo(3);
        assertThat(topico.replicationFactor()).isEqualTo((short) 1);   // no Docker, um broker só
    }
}
