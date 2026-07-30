package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.order.OrderItemRequest;
import guru.springframework.ghd.dto.order.OrderItemResponse;
import guru.springframework.ghd.entities.OrderDetail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {
    OrderDetail toEntity(OrderItemRequest itemRequest);

    OrderItemResponse toResponse(OrderDetail orderDetail);
}