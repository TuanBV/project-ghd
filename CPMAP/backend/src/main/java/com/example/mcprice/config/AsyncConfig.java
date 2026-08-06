package com.example.mcprice.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool rieng cho cac tien trinh chay ngam ngan han (vd tu dong kham pha sitemap khi
 * them doi thu moi), tach biet voi thread pool cua Quartz de khong anh huong lich chay dinh ky.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "discoveryExecutor")
    public Executor discoveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("discovery-");
        executor.initialize();
        return executor;
    }
}
