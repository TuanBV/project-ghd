package guru.springframework.ghd.dto.policy;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductSummaryDTO {
    private String id;
    private String name;
}