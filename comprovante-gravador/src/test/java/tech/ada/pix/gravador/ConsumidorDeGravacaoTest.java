package tech.ada.pix.gravador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.ada.pix.contracts.ComprovanteRequest;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import tech.ada.pix.contracts.TipoChavePix;
import tech.ada.pix.contracts.TipoDocumento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aula 5: o consumidor da fila de gravação.
 *
 * Não precisa do RabbitMQ: o teste chama o método direto, como o Spring faz a cada mensagem que
 * chega. Exceção = sem ack (o RabbitMQ entregaria de novo). O que se confere é o banco do gravador.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",   // não escuta o RabbitMQ de verdade
        "spring.datasource.url=jdbc:h2:mem:gravador-consumidor;DB_CLOSE_DELAY=-1"
})
class ConsumidorDeGravacaoTest {

    @Autowired
    private ConsumidorDeGravacao consumidor;

    @Autowired
    private GravadorService gravador;

    @Autowired
    private ComprovanteRepository repository;

    static GravarComprovanteCommand pedido(UUID id) {
        return new GravarComprovanteCommand(id, new ComprovanteRequest(
                "Giovanni", TipoDocumento.CPF, "50329291076", "2022", "00276", "0",
                new BigDecimal("23.99"), TipoChavePix.CELULAR, "11948755536",
                "Fernando", "churrasco", LocalDateTime.parse("2022-04-10T20:03:57")));
    }

    @Test
    void aMesmaMensagemDuasVezesGravaUmaVezSo() {
        UUID id = UUID.randomUUID();

        consumidor.receber(pedido(id), false);
        consumidor.receber(pedido(id), true);    // a reentrega: "já estava gravado"

        assertThat(repository.findAll()).filteredOn(c -> c.getId().equals(id)).hasSize(1);
    }

    @Test
    void comAChaveLigadaGravaECaiAntesDoAckMasNaoCaiNaReentrega() {
        ConsumidorDeGravacao queCai = new ConsumidorDeGravacao(gravador, true, 0);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> queCai.receber(pedido(id), false)).hasMessageContaining("ANTES do ack");
        assertThat(repository.existsById(id)).isTrue();   // gravou ANTES de cair

        queCai.receber(pedido(id), true);                 // a reentrega termina sem erro: vem o ack
        assertThat(repository.findAll()).filteredOn(c -> c.getId().equals(id)).hasSize(1);
    }

    @Test
    void mensagemSemOsDadosFalhaSemGravarNada() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> consumidor.receber(new GravarComprovanteCommand(id, null), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem os dados");
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void oBancoQuePiscaFalhaNasPrimeirasTentativasEDepoisGrava() {
        ConsumidorDeGravacao comBancoPiscando = new ConsumidorDeGravacao(gravador, false, 2);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> comBancoPiscando.receber(pedido(id), false)).hasMessageContaining("piscou");
        assertThatThrownBy(() -> comBancoPiscando.receber(pedido(id), false)).hasMessageContaining("piscou");
        comBancoPiscando.receber(pedido(id), false);      // a 3ª tentativa passa

        assertThat(repository.existsById(id)).isTrue();
    }
}
