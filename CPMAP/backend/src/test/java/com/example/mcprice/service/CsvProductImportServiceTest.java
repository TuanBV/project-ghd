package com.example.mcprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.mcprice.domain.ImportRow;
import com.example.mcprice.domain.ImportRun;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ImportIssueRepository;
import com.example.mcprice.repository.ImportRowRepository;
import com.example.mcprice.repository.ImportRunRepository;
import com.example.mcprice.repository.ProductRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvProductImportServiceTest {

    @Mock
    private ImportRunRepository importRunRepository;
    @Mock
    private ImportRowRepository importRowRepository;
    @Mock
    private ImportIssueRepository importIssueRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductAliasService productAliasService;
    @Mock
    private AuditService auditService;

    private CsvProductImportService service;

    @BeforeEach
    void setUp() {
        service = new CsvProductImportService(importRunRepository, importRowRepository, importIssueRepository,
                productRepository, productAliasService, auditService);
        when(importRunRepository.findByImportTypeAndFileHash(any(), any())).thenReturn(java.util.Optional.empty());
        when(importRunRepository.save(any(ImportRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(importRowRepository.save(any(ImportRow.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findAllBySkuNormalized(any())).thenReturn(List.of());
    }

    @Test
    void importFile_stockColumnValueOne_mapsToInStock() {
        String csv = "ID,SKU,Ten,\"Con hang?\"\n1,SKU001,San pham A,1\n";
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

        service.importFile("test.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "tester");

        org.mockito.Mockito.verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getAvailability()).isEqualTo("IN_STOCK");
    }

    @Test
    void importFile_stockColumnValueZero_mapsToOutOfStock() {
        String csv = "ID,SKU,Ten,\"Con hang?\"\n1,SKU002,San pham B,0\n";
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

        service.importFile("test.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "tester");

        org.mockito.Mockito.verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getAvailability()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void importFile_withDiacriticsAndQuestionMarkInHeader_stillResolvesStockColumn() {
        // Header thuc te trong product.csv dung dau tieng Viet + dau "?": "Còn hàng?".
        String csv = "ID,SKU,Tên,\"Còn hàng?\"\n1,SKU003,Sản phẩm C,1\n";
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

        service.importFile("test.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "tester");

        org.mockito.Mockito.verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getAvailability()).isEqualTo("IN_STOCK");
    }

    @Test
    void importFile_noStockColumn_fallsBackToUnknown() {
        String csv = "ID,SKU,Ten\n1,SKU004,San pham D\n";
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

        service.importFile("test.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "tester");

        org.mockito.Mockito.verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getAvailability()).isEqualTo("UNKNOWN");
    }
}
