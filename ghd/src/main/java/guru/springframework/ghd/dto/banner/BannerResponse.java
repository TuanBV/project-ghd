package guru.springframework.ghd.dto.banner;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BannerResponse {
    private UUID id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private String position;
    private Integer isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}