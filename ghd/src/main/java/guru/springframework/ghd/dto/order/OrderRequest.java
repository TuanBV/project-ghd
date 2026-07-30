package guru.springframework.ghd.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
public class OrderRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String custName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
    private String custPhone;

    @Email(message = "Email không hợp lệ")
    private String custEmail;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String custAddress;

    private String orderNote;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    private String transferContent;

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Tổng tiền không được để trống")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal totalAmount;
}