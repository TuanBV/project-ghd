package guru.springframework.ghd.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Integer role;
    private Integer delFlag;
    @JsonIgnore
    private String password;
}
