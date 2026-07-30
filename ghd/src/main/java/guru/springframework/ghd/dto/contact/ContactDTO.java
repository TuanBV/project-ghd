package guru.springframework.ghd.dto.contact;

import lombok.*;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactDTO {
    private String title;
    private List<String> list; // Danh sách tên sản phẩm
}