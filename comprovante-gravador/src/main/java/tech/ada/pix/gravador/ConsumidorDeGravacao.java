package tech.ada.pix.gravador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tech.ada.pix.contracts.FilaDeGravacao;
import tech.ada.pix.contracts.GravarComprovanteCommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aula 5: o gravador escuta a fila de gravação (o cozinheiro olhando o trilho).
 *
 * Cada mensagem é um GravarComprovanteCommand, publicado pelo emissor. O Spring manda o ack
 * (o "terminei") quando este método termina SEM erro: só aí a mensagem sai da fila. Se der erro,
 * não tem ack, e o RabbitMQ entrega a mesma mensagem de novo (a reentrega).
 *
 * A mesma mensagem pode chegar duas vezes. Por isso, quem grava é o GravadorService, que é
 * idempotente desde a Aula 2: olha o id do comprovante antes de gravar.
 */
@Component
public class ConsumidorDeGravacao {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorDeGravacao.class);

    private final GravadorService gravador;
    private final boolean falharAntesDoAck;
    private final int falhasPassageiras;
    private final Map<UUID, Integer> tentativas = new ConcurrentHashMap<>(); // ⚠️ só para a demo

    public ConsumidorDeGravacao(GravadorService gravador,
                                @Value("${demo.falhar-antes-do-ack:false}") boolean falharAntesDoAck,
                                @Value("${demo.falhas-passageiras:0}") int falhasPassageiras) {
        this.gravador = gravador;
        this.falharAntesDoAck = falharAntesDoAck;
        this.falhasPassageiras = falhasPassageiras;
    }

    @RabbitListener(queues = FilaDeGravacao.FILA)
    public void receber(GravarComprovanteCommand comando,
                        @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean reentrega) {
        UUID id = comando.identificadorComprovante();
        log.info("Recebi pela fila o pedido de gravação do comprovante {} (reentrega: {})",
                id, Boolean.TRUE.equals(reentrega) ? "sim" : "não");
        try {
            gravar(comando);
        } catch (RuntimeException erro) {
            // Uma linha só no log. A exceção continua subindo: é ela que diz ao Spring "não mande o ack".
            log.warn("Deu erro no comprovante {}: {}. Sem ack.", id, erro.getMessage());
            throw erro;
        }
    }

    private void gravar(GravarComprovanteCommand comando) {
        UUID id = comando.identificadorComprovante();
        if (comando.dados() == null) {
            // Defeito na própria mensagem: tentar de novo nunca vai resolver (falha permanente).
            throw new IllegalArgumentException("o pedido veio sem os dados do comprovante");
        }

        bancoPiscando(id); // ⚠️ só para o desafio extra

        if (gravador.gravar(comando)) {
            log.info("Gravei o comprovante {}", id);
            if (falharAntesDoAck) { // ⚠️ só para o passo 3
                throw new IllegalStateException("⚠️ demo: caí depois de gravar e ANTES do ack");
            }
        } else {
            log.info("O comprovante {} já estava gravado: nada a fazer", id);
        }
    }

    /** Desafio extra: finge que o banco "piscou" nas primeiras N tentativas de cada comprovante. */
    private void bancoPiscando(UUID id) {
        int tentativa = tentativas.merge(id, 1, Integer::sum);
        if (tentativa <= falhasPassageiras) {
            throw new IllegalStateException("⚠️ demo: o banco piscou (tentativa " + tentativa + ")");
        }
    }
}
