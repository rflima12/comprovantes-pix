package tech.ada.pix.reacoes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tech.ada.pix.contracts.ComprovanteGravadoEvent;
import tech.ada.pix.contracts.TopicoComprovanteGravado;


@Component
public class NotificacaoSms {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoSms.class);

    @KafkaListener(topics = TopicoComprovanteGravado.NOME, groupId = "notificacao")
    public void quandoGravado(ComprovanteGravadoEvent evento,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int particao,
                              @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("SMS enviado: seu PIX de R$ {} foi concluído (comprovante {}) [partição {}, offset {}]",
                evento.valorTransacao(), evento.identificadorComprovante(), particao, offset);
    }
}
