package guru.springframework.ghd.events;

public record UserRegisteredEvent(String userId, String username, String email) {
}
