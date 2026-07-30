package guru.springframework.ghd.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CategoryRequest {
    @NotNull
    @NotBlank(message = "Please input title")
    private String title;

    private Integer priority;

    private MultipartFile logo;
}
