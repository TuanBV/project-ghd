package guru.springframework.ghd.dto.slider;

import lombok.*;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SliderResponse {
    private UUID id;
    private String title;
    private String description;
    private String imageUrl;
    private String linkUrl;
    private Integer position;
    private Boolean isActive;
}
