package tech.ada.pix.gravador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tech.ada.pix.contracts.ComprovanteGravadoEvent;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import tech.ada.pix.contracts.TopicoComprovanteGravado;

import java.time.LocalDateTime;

/**
 * Aula 6: o gravador é o PRODUTOR do fato "o comprovante foi gravado".
 *
 * Quem sabe que gravou é o gravador, então é ele que publica. Ele não sabe quem vai ler
 * (o SMS, o antifraude...): publica no tópico, e cada grupo lê no seu ritmo.
 *
 * Só publica DEPOIS de gravar de verdade (quem chama é o ConsumidorDeGravacao e o
 * GravadorController). No "já estava gravado", não publica: não aconteceu nada de novo.
 *
 * A CHAVE é o id do comprovante: o Kafka faz uma conta com a chave e escolhe a partição.
 * Mesma chave, mesma partição: tudo de um comprovante fica em ordem.
 *
 * O send não espera: volta na hora. A confirmação do Kafka (o ack do produtor, "guardei")
 * chega depois, no whenComplete. Só aí o gravador sabe a partição e o offset que o fato ganhou.
 */
@Component
public class PublicadorDoFato {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDoFato.class);

    private final KafkaTemplate<String, ComprovanteGravadoEvent> kafka;

    public PublicadorDoFato(KafkaTemplate<String, ComprovanteGravadoEvent> kafka) {
        this.kafka = kafka;
    }

    /** Publica o fato "este comprovante foi gravado", com o valor junto. */
    public void publicar(GravarComprovanteCommand gravado) {
        var evento = new ComprovanteGravadoEvent(
                gravado.identificadorComprovante(), gravado.dados().valorTransacao(), LocalDateTime.now());
        String chave = evento.identificadorComprovante().toString();
        kafka.send(TopicoComprovanteGravado.NOME, chave, evento)   // o tópico, a chave e o fato
                .whenComplete((resultado, erro) -> confirmar(chave, resultado, erro));
    }

    private void confirmar(String chave, SendResult<String, ComprovanteGravadoEvent> resultado, Throwable erro) {
        if (erro != null) {
            // O comprovante está gravado, mas o fato não saiu. É a "escrita dupla" (a cola da aula).
            log.error("Não consegui publicar o fato do comprovante {}: {}", chave, erro.getMessage());
            return;
        }
        var onde = resultado.getRecordMetadata();
        log.info("Publiquei o fato no tópico {}: comprovante {} (partição {}, offset {})",
                onde.topic(), chave, onde.partition(), onde.offset());
    }
}
