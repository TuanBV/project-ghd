package guru.springframework.ghd.dto.user;

import guru.springframework.ghd.validations.EmailCommon;
import guru.springframework.ghd.validations.PhoneNumberCommon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @EmailCommon
    private String email;

    @PhoneNumberCommon
    private String phone;

    @NotBlank
    @NotNull
    private String fullName;
}
