package guru.springframework.ghd.dto.product;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductImageResponse {
    private String id;
    private String productId;
    private String imageUrl;
}
