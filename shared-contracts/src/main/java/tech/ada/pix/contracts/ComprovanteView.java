package tech.ada.pix.contracts;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um comprovante já gravado, do jeito que a consulta mostra (Aula 4).
 *
 * <p>O gravador devolve isto no {@code GET /interno/comprovantes/{id}}, e a consulta repassa
 * para o cliente no {@code GET /comprovantes/{id}}. É também o que fica guardado no cache.</p>
 */
public record ComprovanteView(
        UUID identificadorComprovante,
        String nome,
        String numeroDocumento,
        BigDecimal valorTransacao,
        String chavePixDestino,
        LocalDateTime dataHoraTransacao,
        LocalDateTime dataHoraGravacao
) {
}
