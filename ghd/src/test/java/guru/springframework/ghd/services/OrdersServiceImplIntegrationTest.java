package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.dto.order.OrderCreationResult;
import guru.springframework.ghd.dto.order.OrderItemRequest;
import guru.springframework.ghd.dto.order.OrderRequest;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * buildPaymentUrl chỉ ký/build 1 chuỗi URL, không gọi mạng thật ra VNPay - nên test được
 * đầy đủ nhánh CARD/INSTALLMENT bằng property giả (không cần sandbox thật).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "vnpay.tmn-code=TESTCODE",
        "vnpay.hash-secret=integration-test-secret",
        "vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
})
class OrdersServiceImplIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    // Không có Kafka trong AbstractIntegrationTest (chỉ Testcontainers MySQL+Redis) -
    // mock để tránh gọi mạng thật ra localhost:9092 lúc afterCommit publish OrderCreatedEvent.
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Product saveTestProduct(int stockQty) {
        Product product = Product.builder()
                .categoryId("test-category")
                .brandId("test-brand")
                .policyId("test-policy")
                .title("Test Product")
                .sku("TEST-SKU-" + java.util.UUID.randomUUID())
                .price(BigDecimal.valueOf(1_000_000))
                .salePrice(BigDecimal.valueOf(1_000_000))
                .stockQty(stockQty)
                .build();
        return productRepository.save(product);
    }

    private OrderRequest buildOrderRequest(String paymentMethod, String productId) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(2);
        item.setPrice(BigDecimal.valueOf(500_000));

        OrderRequest request = new OrderRequest();
        request.setCustName("Nguyen Van A");
        request.setCustPhone("0900000000");
        request.setCustAddress("123 Test Street");
        request.setPaymentMethod(paymentMethod);
        request.setItems(List.of(item));
        request.setTotalAmount(BigDecimal.valueOf(1_000_000));
        return request;
    }

    @Test
    void cardPaymentDoesNotTouchStockAndReturnsPaymentUrl() {
        Product product = saveTestProduct(5);

        OrderCreationResult result = ordersService.createOrder(buildOrderRequest("CARD", product.getId()), "127.0.0.1");

        assertThat(result.paymentUrl()).isNotBlank();
        assertThat(result.paymentUrl()).contains("vnp_TxnRef=");

        Orders order = ordersRepository.findByOrderId(result.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(orderDetailRepository.findAllByOrderId(result.orderId())).hasSize(1);
        // Chưa trừ kho - chỉ trừ lúc IPN xác nhận thành công (xem PaymentServiceImplIntegrationTest)
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(5);
    }

    @Test
    void installmentPaymentDoesNotTouchStockAndReturnsPaymentUrl() {
        Product product = saveTestProduct(5);

        OrderCreationResult result = ordersService.createOrder(buildOrderRequest("INSTALLMENT", product.getId()), "127.0.0.1");

        assertThat(result.paymentUrl()).isNotBlank();
        Orders order = ordersRepository.findByOrderId(result.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(5);
    }

    @Test
    void codOrderKeepsExistingBehaviorNoPaymentUrlStockDecrementedImmediately() {
        Product product = saveTestProduct(5);

        OrderCreationResult result = ordersService.createOrder(buildOrderRequest("COD", product.getId()), "127.0.0.1");

        assertThat(result.paymentUrl()).isNull();
        Orders order = ordersRepository.findByOrderId(result.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(3);
    }
}
