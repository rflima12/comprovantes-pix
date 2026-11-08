package tech.ada.pix.consulta;

import tech.ada.pix.contracts.ComprovanteView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Cliente HTTP do gravador: lê o comprovante na fonte da verdade (Aula 4).
 *
 * A consulta não abre o banco do gravador: pede pela porta da frente dele (Aula 1: cada
 * serviço com o seu banco).
 */
@Component
public class GravadorClient {

    private final RestClient http;

    public GravadorClient(RestClient.Builder builder,
                          @Value("${gravador.base-url}") String baseUrl,
                          @Value("${gravador.timeout-ms}") int timeoutMs) {
        SimpleClientHttpRequestFactory tempoLimite = new SimpleClientHttpRequestFactory();
        tempoLimite.setConnectTimeout(timeoutMs);
        tempoLimite.setReadTimeout(timeoutMs);
        // O builder do Spring já vem com o Jackson do serviço (JSON em snake_case).
        this.http = builder.baseUrl(baseUrl).requestFactory(tempoLimite).build();
    }

    /** Vazio se o gravador responder 404. Lança RestClientException se ele estiver fora. */
    public Optional<ComprovanteView> buscar(UUID id) {
        ComprovanteView comprovante = http.get().uri("/interno/comprovantes/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, (pedido, resposta) -> { }) // 404 não é erro: é "não tenho"
                .body(ComprovanteView.class);
        return Optional.ofNullable(comprovante);
    }
}
