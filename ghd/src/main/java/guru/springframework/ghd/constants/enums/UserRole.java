package guru.springframework.ghd.constants.enums;

public enum UserRole {
    ADMIN(0),
    USER(1);
    private final Integer value;

    UserRole(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

}