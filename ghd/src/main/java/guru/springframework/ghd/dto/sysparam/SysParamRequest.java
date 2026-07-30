package guru.springframework.ghd.dto.sysparam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysParamRequest {
    @NotNull
    @NotBlank
    private String paramKey;
    @NotNull
    @NotBlank
    private String paramValue;
}
