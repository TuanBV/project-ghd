package guru.springframework.ghd.dto.category;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {
    private UUID id;
    private String title;
    private String logo;
    private Integer delFlag;
    private Integer priority;
    private String slug;
}
