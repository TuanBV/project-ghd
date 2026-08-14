package guru.springframework.ghd.listeners;

import guru.springframework.ghd.events.PageViewEvent;
import guru.springframework.ghd.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static guru.springframework.ghd.config.KafkaTopicConfig.ANALYTICS_EVENTS_TOPIC;

@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private final AnalyticsService analyticsService;

    @KafkaListener(topics = ANALYTICS_EVENTS_TOPIC, groupId = "ghd-analytics", concurrency = "3")
    public void onPageView(PageViewEvent event) {
        analyticsService.persist(event);
    }
}
