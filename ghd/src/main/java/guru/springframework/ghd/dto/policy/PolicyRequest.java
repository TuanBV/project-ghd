package guru.springframework.ghd.dto.policy;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter

public class PolicyRequest {
    @NotBlank(message = "Tên chính sách không được để trống")
    @Size(max = 255, message = "Tên không được vượt quá 255 ký tự")
    private String packageName;

    private List<String> policies;

    private List<String> afterSales;

    private List<String> gifts;

    private List<String> appliedProductIds;

    @Min(0) @Max(1)
    private Integer isActive;
}