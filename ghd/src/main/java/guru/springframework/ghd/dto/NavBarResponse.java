package guru.springframework.ghd.dto;

import guru.springframework.ghd.dto.brand.BrandResponse;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NavBarResponse {
    private UUID id;
    private String title;
    private String logo;
    private List<BrandResponse> childs;
    private String slug;
}
