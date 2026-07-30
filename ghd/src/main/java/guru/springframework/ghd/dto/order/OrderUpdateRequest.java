package guru.springframework.ghd.dto.order;

import guru.springframework.ghd.constants.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderUpdateRequest {
    private OrderStatus status;
    private String adminNote;
}