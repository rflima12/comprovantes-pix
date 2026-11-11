package tech.ada.pix.gravador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aula 4: o gravador também lê. É a fonte da verdade da consulta.
 * (Banco separado do GravadorIdempotenciaTest, que conta as linhas da tabela.)
 */
@SpringBootTest(properties = {
        "demo.leitura-lenta-ms=0",
        "spring.rabbitmq.listener.simple.auto-startup=false",   // não escuta o RabbitMQ de verdade
        "spring.datasource.url=jdbc:h2:mem:gravador-leitura;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class GravadorLeituraTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void devolveOComprovanteGravado() throws Exception {
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
                .andExpect(status().isCreated());

        mvc.perform(get("/interno/comprovantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador_comprovante").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Giovanni"))
                .andExpect(jsonPath("$.valor_transacao").value(23.99));
    }

    @Test
    void comprovanteQueNaoFoiGravadoDa404() throws Exception {
        mvc.perform(get("/interno/comprovantes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
