package tech.ada.pix.gravador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mandar o MESMO comando duas vezes não pode gravar dois comprovantes. */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",   // não escuta o RabbitMQ de verdade
        "spring.kafka.admin.auto-create=false",                 // não cria o tópico no Kafka de verdade
})
@AutoConfigureMockMvc
class GravadorIdempotenciaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ComprovanteRepository repository;

    @MockBean
    private PublicadorDoFato fato;   // o Kafka de mentira: o teste não precisa do Kafka rodando

    @Test
    void oMesmoComandoDuasVezesGravaUmaVezSo() throws Exception {
        UUID id = UUID.randomUUID();
        String comando = """
                {"identificador_comprovante":"%s",
                 "dados":{"nome":"Giovanni","tipo_documento":"CPF","numero_documento":"50329291076",
                          "numero_agencia":"2022","numero_conta":"00276","digito_verificador_conta":"0",
                          "valor_transacao":23.99,"tipo_chave_pix_destino":"CELULAR","chave_pix_destino":"11948755536",
                          "nome_cliente_destino":"Fernando","identificacao_pix":"churrasco",
                          "data_hora_transacao":"2022-04-10T20:03:57"}}
                """.formatted(id);

        mvc.perform(post("/interno/comprovantes").contentType(MediaType.APPLICATION_JSON).content(comando))
                .andExpect(status().isCreated());   // 1ª vez: gravou
        mvc.perform(post("/interno/comprovantes").contentType(MediaType.APPLICATION_JSON).content(comando))
                .andExpect(status().isOk());        // 2ª vez: "já tenho", não grava de novo

        assertThat(repository.findById(id)).isPresent();
        assertThat(repository.findById(id).get().getNome()).isEqualTo("Giovanni");
        assertThat(repository.count()).isEqualTo(1);
        verify(fato, times(1)).publicar(any());   // gravou uma vez: um fato só (Aula 6)
    }
}
