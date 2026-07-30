package guru.springframework.ghd.dto.slider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255)
    private String title;

    private String description;

    private MultipartFile imageFile;

    private String linkUrl;

    private Integer position;

    private Boolean isActive;
}
