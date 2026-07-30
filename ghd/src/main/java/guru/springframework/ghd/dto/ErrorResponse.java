package guru.springframework.ghd.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse<T> {
    private int status;
    private String message;
    private T data;
}
