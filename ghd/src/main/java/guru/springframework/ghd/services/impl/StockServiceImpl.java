package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.entities.OrderDetail;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.events.OrderCreatedEvent;
import guru.springframework.ghd.exceptions.InsufficientStockException;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    @Override
    public List<OrderCreatedEvent.OrderItemInfo> decrementStockForOrder(String orderId) {
        List<OrderDetail> details = orderDetailRepository.findAllByOrderId(orderId);
        List<OrderCreatedEvent.OrderItemInfo> eventItems = new ArrayList<>();

        for (OrderDetail detail : details) {
            Product product = productRepository.findByIdForUpdate(detail.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + detail.getProductId()));

            if (product.getStockQty() < detail.getQuantity()) {
                throw new InsufficientStockException(
                        "Sản phẩm " + product.getTitle() + " không đủ hàng trong kho!");
            }

            product.setStockQty(product.getStockQty() - detail.getQuantity());
            Integer soldCount = product.getSoldCount() == null ? 0 : product.getSoldCount();
            product.setSoldCount(soldCount + detail.getQuantity());
            productRepository.save(product);

            eventItems.add(new OrderCreatedEvent.OrderItemInfo(product.getTitle(), detail.getQuantity(), detail.getPrice()));
        }

        return eventItems;
    }
}
