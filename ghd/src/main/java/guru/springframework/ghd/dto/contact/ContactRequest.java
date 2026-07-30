package guru.springframework.ghd.dto.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ContactRequest {
    @NotNull
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "(0[3|5|7|8|9])([0-9]{8})\\b", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    private String serviceType;

    @NotBlank(message = "Vui lòng nhập nội dung")
    private String message;
}
