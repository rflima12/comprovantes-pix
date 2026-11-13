package tech.ada.pix.contracts;

/**
 * Aula 6: os nomes do tópico do fato "o comprovante foi gravado", no Kafka.
 *
 * <p>Fazem parte do contrato entre o gravador (que publica) e quem lê (o comprovante-reacoes),
 * como o {@link ComprovanteGravadoEvent}. Por isso moram aqui, como a {@link FilaDeGravacao}
 * da Aula 5: os dois lados usam os mesmos nomes.</p>
 *
 * <pre>
 *   gravador ──publica──► comprovante-gravado
 *                           partição 0: [0][1][2] ...
 *                           partição 1: [0][1] ...
 *                           partição 2: [0] ...
 *                              │                  │
 *                    grupo "notificacao"   grupo "antifraude"   (cada grupo lê tudo, com o seu marcador)
 * </pre>
 *
 * <p>A chave de cada mensagem é o id do comprovante: tudo de um comprovante cai na mesma
 * partição, em ordem.</p>
 */
public final class TopicoComprovanteGravado {

    /** O tópico: o lance a lance dos comprovantes gravados. */
    public static final String NOME = "comprovante-gravado";

    /** 3 partições: até 3 cópias de um mesmo grupo lendo ao mesmo tempo. */
    public static final int PARTICOES = 3;

    private TopicoComprovanteGravado() {
        // só constantes
    }
}
