package guru.springframework.ghd.dto.product;

import lombok.Data;

@Data
public class GalleryOrderItem {
    private String type;      // old | new
    private String imageUrl;  // ảnh cũ
    private String fileId;    // ảnh mới
    private Integer sortOrder;
}