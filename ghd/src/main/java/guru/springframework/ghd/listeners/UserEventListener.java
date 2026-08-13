package guru.springframework.ghd.listeners;

import guru.springframework.ghd.events.UserRegisteredEvent;
import guru.springframework.ghd.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static guru.springframework.ghd.config.KafkaTopicConfig.USER_EVENTS_TOPIC;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    @KafkaListener(topics = USER_EVENTS_TOPIC, groupId = "ghd-notifications")
    public void onUserRegistered(UserRegisteredEvent event) {
        emailService.sendWelcomeEmail(event.email(), event.username());
    }
}
