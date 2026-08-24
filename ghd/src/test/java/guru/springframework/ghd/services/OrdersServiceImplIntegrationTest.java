package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.dto.order.OrderCreationResult;
import guru.springframework.ghd.dto.order.OrderItemRequest;
import guru.springframework.ghd.dto.order.OrderRequest;
import guru.springframework.ghd.dto.order.OrderResponse;
import guru.springframework.ghd.dto.sysparam.SysParamRequest;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.repositories.OrderDetailRepository;
import guru.springframework.ghd.repositories.OrdersRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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

    @Autowired
    private SysParamService sysParamService;

    // CARD/INSTALLMENT mặc định TẮT từ V30 (VNPay chưa test xong với sandbox thật -
    // xem PaymentMethodConfigService) - bật lại ở đây để 2 test dưới vẫn kiểm tra đúng
    // luồng tạo đơn CARD/INSTALLMENT độc lập với cấu hình bật/tắt của admin.
    private void enablePaymentMethod(String key) {
        SysParamRequest req = new SysParamRequest();
        req.setParamKey(key);
        req.setParamValue("true");
        sysParamService.updateSysParam(List.of(req));
    }

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
        enablePaymentMethod("payment.card.enabled");
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
        enablePaymentMethod("payment.installment.enabled");
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

    // Bug thật đã tái hiện: searchOrders/OrdersRepository.search (trước đây
    // findAllNative - native query + Pageable có Sort) ném lỗi 500 trên MỌI request vì
    // sortField mặc định "created_date" luôn tạo ra 1 Sort khác unsorted(). Test này
    // dùng ĐÚNG tham số mặc định của OrderController/DefaultPage để không tái diễn.
    @Test
    void searchOrdersWithDefaultSortDoesNotThrow() {
        Product product = saveTestProduct(5);
        ordersService.createOrder(buildOrderRequest("COD", product.getId()), "127.0.0.1");

        Page<OrderResponse> page = ordersService.searchOrders(
                null, null, null, null, null, null,
                DefaultPage.CREATED_DATE, DefaultPage.DESC, 1, 10);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchOrdersFiltersByOrderId() {
        Product product = saveTestProduct(5);
        OrderCreationResult result = ordersService.createOrder(buildOrderRequest("COD", product.getId()), "127.0.0.1");

        Page<OrderResponse> page = ordersService.searchOrders(
                result.orderId(), null, null, null, null, null,
                DefaultPage.CREATED_DATE, DefaultPage.DESC, 1, 10);

        assertThat(page.getContent()).extracting(OrderResponse::getId).containsExactly(result.orderId());
    }

    @Test
    void searchOrdersFiltersByCustomerNameWhenOrderIdNotGiven() {
        Product product = saveTestProduct(5);
        ordersService.createOrder(buildOrderRequest("COD", product.getId()), "127.0.0.1");

        Page<OrderResponse> page = ordersService.searchOrders(
                null, "Nguyen Van A", null, null, null, null,
                DefaultPage.CREATED_DATE, DefaultPage.DESC, 1, 10);

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(o -> o.getCustomerName().contains("Nguyen Van A"));
    }

    @Test
    void searchOrdersLeavesPaymentStatusNullForCod() {
        Product product = saveTestProduct(5);
        OrderCreationResult result = ordersService.createOrder(buildOrderRequest("COD", product.getId()), "127.0.0.1");

        Page<OrderResponse> page = ordersService.searchOrders(
                result.orderId(), null, null, null, null, null,
                DefaultPage.CREATED_DATE, DefaultPage.DESC, 1, 10);

        assertThat(page.getContent().get(0).getPaymentStatus()).isNull();
    }
}
