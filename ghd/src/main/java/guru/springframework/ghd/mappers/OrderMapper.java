package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.order.OrderDetailResponse;
import guru.springframework.ghd.dto.order.OrderRequest;
import guru.springframework.ghd.dto.order.OrderResponse;
import guru.springframework.ghd.entities.Orders;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Orders order);

    Orders toEntity(OrderRequest orderRequest);

    OrderDetailResponse toOrderDetailResponse(Orders order);

}