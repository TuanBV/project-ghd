package guru.springframework.ghd.dto.brand;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BrandResponse {
    private UUID id;
    private String title;
    private String logo;
    private Integer delFlag;
    private String slug;
}
