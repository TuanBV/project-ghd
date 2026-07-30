package guru.springframework.ghd.dto.auth;

import guru.springframework.ghd.dto.ResponseBody;
import guru.springframework.ghd.entities.User;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse extends ResponseBody {
    private User data;
    private String token;
}
