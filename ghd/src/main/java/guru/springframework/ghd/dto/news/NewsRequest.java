package guru.springframework.ghd.dto.news;

import guru.springframework.ghd.validations.UniqueSlug;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@UniqueSlug(message = "Slug này đã bị trùng")
public class NewsRequest {

    private String id;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Slug không được để trống")
    private String slug;

    @Size(max = 1000, message = "Tóm tắt không được vượt quá 1000 ký tự")
    private String summary;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private MultipartFile thumbnailFile;

    private String thumbnail;

    private String categoryId;

    private String brandId;

    @NotBlank(message = "Trạng thái bài viết không được để trống")
    private String status;

    @NotNull(message = "Vui lòng chọn chế độ nổi bật")
    private Boolean isFeatured;

    @Size(max = 200, message = "Meta Title không nên quá 200 ký tự")
    private String metaTitle;

    private String metaKeyword;

    @Size(max = 1000, message = "Tóm tắt không được vượt quá 1000 ký tự")
    private String metaDesc;

    @NotBlank(message = "Loại bài viết không xác định")
    private String postType;
}