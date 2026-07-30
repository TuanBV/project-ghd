package guru.springframework.ghd.constants.enums;

public enum DelFlag {
    ACTIVE(0), NOT_ACTIVE(1);

    private final Integer value;

    DelFlag(Integer value) {
        this.value = value;
    }

    public Integer get() {
        return value;
    }
}
