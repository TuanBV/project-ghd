package guru.springframework.ghd.dto.banner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
    private String title;

    private String description;

    private MultipartFile imageFile;

    @Size(max = 255, message = "Link URL không quá 255 ký tự")
    private String linkUrl;

    @Size(max = 50)
    private String position;

    private Integer isActive;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}