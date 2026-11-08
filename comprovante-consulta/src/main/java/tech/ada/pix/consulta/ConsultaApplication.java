package tech.ada.pix.consulta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching // Aula 4: liga o @Cacheable. Sem isto, a anotação é ignorada em silêncio.
public class ConsultaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsultaApplication.class, args);
    }
}
