package guru.springframework.ghd.dto.contact;

import guru.springframework.ghd.constants.enums.ContactStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactResponse {
    private UUID id;
    private Integer version;
    private String fullName;
    private String phone;
    private String serviceType;
    private String message;
    private ContactStatus status;
    private String note;
    private LocalDateTime createdDate;
}