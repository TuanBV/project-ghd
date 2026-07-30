package guru.springframework.ghd.dto.policy;

import lombok.*;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PolicyCustomResponse {
    private String id;

    private String packageName;

    private List<String> policies;

    private List<String> afterSales;

    private List<String> gifts;

    private List<ProductSummaryDTO> appliedProductIds;

    private Integer isActive;
}
