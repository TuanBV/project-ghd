package com.example.mcprice.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Mac dinh Spring Data JPA auditing dung LocalDateTime cho @CreatedDate/@LastModifiedDate,
 * khong convert duoc sang OffsetDateTime (kieu dung xuyen suot entity trong he thong nay).
 * Cung cap DateTimeProvider tra ve OffsetDateTime truc tiep de tranh loi convert.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
