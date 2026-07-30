package guru.springframework.ghd.dto.sysparam;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SysParamResponse {
    private Long id;
    private String paramKey;
    private String paramValue;
    private String paramName;
    private String groupCode;
    private String description;
}
