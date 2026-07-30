package guru.springframework.ghd.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderDetailProjection {
    String getProductId();
    String getProductName();
    String getCategoryName();
    String getSku();
    String getColor();
    String getSize();
    String getImage();
    Integer getQuantity();
    Double getPrice();
    Double getTotalItemAmount();
}
