package guru.springframework.ghd.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserImagesResponse {
    private UUID id;
    private String url;
    private String userId;
}
