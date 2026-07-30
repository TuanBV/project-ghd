package guru.springframework.ghd.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class BrandRequest {
    @NotNull
    @NotBlank(message = "Please input title")
    private String title;

    private MultipartFile logo;
}
