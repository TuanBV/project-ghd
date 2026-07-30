package guru.springframework.ghd.dto.user;

import guru.springframework.ghd.validations.EmailCommon;
import guru.springframework.ghd.validations.PhoneNumberCommon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserRequest {
    @NotNull
    @NotBlank(message = "Please input username")
    private String username;

    @EmailCommon
    private String email;

    @PhoneNumberCommon
    private String phone;

    private Integer role;

    private MultipartFile avatar;

    private String password;
}
