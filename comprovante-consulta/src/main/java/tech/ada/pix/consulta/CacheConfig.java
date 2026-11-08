package tech.ada.pix.consulta;

import com.fasterxml.jackson.databind.ObjectMapper;
import tech.ada.pix.contracts.ComprovanteView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Como o cache guarda os comprovantes no Redis (Aula 4).
 *
 * - Cada comprovante fica no cache por {@code consulta.cache.ttl} (o TTL). Depois, o Redis apaga sozinho.
 * - O comprovante vai em JSON, para dar para ler no redis-cli.
 * - A chave é "comprovantes::" + o id do comprovante.
 */
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration comoGuardarNoRedis(ObjectMapper jackson,
                                                      @Value("${consulta.cache.ttl}") Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(jackson, ComprovanteView.class)));
    }
}
