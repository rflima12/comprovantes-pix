package tech.ada.pix.contracts;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aula 6: o FATO "o comprovante foi gravado", publicado pelo gravador no tópico
 * {@link TopicoComprovanteGravado#NOME} do Kafka, depois de gravar de verdade.
 *
 * <p>É um evento (aconteceu, no passado), e não um comando (faça). Vários grupos leem o mesmo
 * fato (o SMS, o antifraude...), cada um no seu ritmo, e o gravador não conhece nenhum deles.</p>
 *
 * <p>Leva o valor junto, como o evento do Spring da Aula 3: quem lê não precisa perguntar de
 * volta ao gravador quanto foi o PIX.</p>
 */
public record ComprovanteGravadoEvent(
        UUID identificadorComprovante,
        BigDecimal valorTransacao,
        LocalDateTime dataHoraGravacao
) {
}
