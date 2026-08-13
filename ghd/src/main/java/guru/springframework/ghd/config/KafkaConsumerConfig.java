package guru.springframework.ghd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Retries a failed listener invocation 3 times (2s apart), then logs and moves on -
 * mirrors the log-and-continue behavior these notification flows already had before Kafka.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Bỏ qua message sau khi retry thất bại - topic={}, partition={}, offset={}: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage(), exception),
                new FixedBackOff(2000L, 3L));
        return errorHandler;
    }
}
