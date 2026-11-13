package tech.ada.pix.reacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aula 6: o serviço das reações. Até a Aula 5, elas moravam dentro do emissor e ouviam um evento
 * do Spring, que não saía do processo. Agora cada reação lê o fato "comprovante gravado" no Kafka,
 * no seu próprio consumer group.
 *
 * Sem porta e sem banco: este serviço só escuta o tópico.
 */
@SpringBootApplication
public class ReacoesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReacoesApplication.class, args);
    }
}
