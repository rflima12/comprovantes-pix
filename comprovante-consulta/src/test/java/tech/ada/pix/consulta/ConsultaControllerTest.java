package tech.ada.pix.consulta;

import tech.ada.pix.contracts.ComprovanteView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aula 4: a consulta repassa o que o gravador tem. O gravador aqui é de mentira (Mockito), e o
 * cache fica na memória do teste (spring.cache.type=simple): os testes não precisam do Redis.
 */
@SpringBootTest(properties = "spring.cache.type=simple")
@AutoConfigureMockMvc
class ConsultaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GravadorClient gravador;

    @Test
    void comprovanteGravadoDa200ComOsDados() throws Exception {
        UUID id = UUID.randomUUID();
        when(gravador.buscar(id)).thenReturn(Optional.of(new ComprovanteView(id, "Giovanni", "50329291076",
                new BigDecimal("23.99"), "11948755536", LocalDateTime.of(2022, 4, 10, 20, 3, 57), LocalDateTime.now())));

        mvc.perform(get("/comprovantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador_comprovante").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Giovanni"))
                .andExpect(jsonPath("$.valor_transacao").value(23.99));
    }

    @Test
    void comprovanteQueNaoExisteDa404() throws Exception {
        UUID id = UUID.randomUUID();
        when(gravador.buscar(id)).thenReturn(Optional.empty());

        mvc.perform(get("/comprovantes/{id}", id))
                .andExpect(status().isNotFound());
    }
}
