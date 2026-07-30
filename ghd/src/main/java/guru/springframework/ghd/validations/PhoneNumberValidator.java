package guru.springframework.ghd.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumberCommon, String> {

    private static final String PHONE_REGEX = "^0[35789][0-9]{8}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(PHONE_REGEX);
    }
}