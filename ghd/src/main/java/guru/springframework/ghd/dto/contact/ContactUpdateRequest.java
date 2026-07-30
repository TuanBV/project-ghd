package guru.springframework.ghd.dto.contact;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactUpdateRequest {
    @Pattern(regexp = "NEW|PROCESSING|COMPLETED|CANCELLED", message = "Vui lòng chọn đúng trạng thái")
    private String status;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;
}
