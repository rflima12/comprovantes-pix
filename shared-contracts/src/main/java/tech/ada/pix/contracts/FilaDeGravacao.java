package tech.ada.pix.contracts;

/**
 * Aula 5: os nomes do caminho da mensagem de gravação no RabbitMQ.
 *
 * <p>Fazem parte do contrato entre o emissor (que publica) e o gravador (que consome), como o
 * {@link GravarComprovanteCommand}. Por isso moram aqui: os dois lados usam os mesmos nomes.</p>
 *
 * <pre>
 *   gravacao.ex ──"gravacao"──► gravacao.q ──► gravador
 *                                   │ falhou de vez
 *                                   ▼
 *   gravacao.dlx ──"gravacao"──► gravacao.dlq   (alguém olha)
 * </pre>
 */
public final class FilaDeGravacao {

    /** A exchange (o balcão): onde o emissor publica. */
    public static final String EXCHANGE = "gravacao.ex";

    /** A routing key (a etiqueta) que vai em cada mensagem de gravação. */
    public static final String ROUTING_KEY = "gravacao";

    /** A fila (o trilho): onde o pedido de gravação espera o gravador. */
    public static final String FILA = "gravacao.q";

    /** A exchange da DLQ: recebe as mensagens que falharam de vez. */
    public static final String DLX = "gravacao.dlx";

    /** A DLQ (a caixinha do gerente): onde as mensagens que falharam de vez ficam, para alguém olhar. */
    public static final String DLQ = "gravacao.dlq";

    private FilaDeGravacao() {
        // só constantes
    }
}
