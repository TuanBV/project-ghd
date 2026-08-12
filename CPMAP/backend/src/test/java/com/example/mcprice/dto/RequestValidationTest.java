package com.example.mcprice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Kiem tra thuan Bean Validation (khong can Spring context) cho cac request DTO da them/sua
 * annotation trong lan chuan hoa REST API — dam bao annotation THUC SU chan duoc input sai,
 * khong chi ton tai tren code ma khong duoc ap dung (vd bug thieu @Valid o controller da gap
 * truoc do voi UpdateCronRequest).
 */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void updateCronRequest_blankFields_violatesNotBlank() {
        Set<ConstraintViolation<UpdateCronRequest>> violations = validator.validate(new UpdateCronRequest("", ""));
        assertThat(violations).hasSize(2);
    }

    @Test
    void updateCronRequest_validValues_noViolation() {
        assertThat(validator.validate(new UpdateCronRequest("0 0 * * * ?", "Asia/Ho_Chi_Minh"))).isEmpty();
    }

    @Test
    void updateMatchStatusRequest_invalidStatus_violatesPattern() {
        assertThat(validator.validate(new UpdateMatchStatusRequest("DELETED", null))).isNotEmpty();
    }

    @Test
    void updateMatchStatusRequest_validStatuses_noViolation() {
        assertThat(validator.validate(new UpdateMatchStatusRequest("MANUALLY_CONFIRMED", null))).isEmpty();
        assertThat(validator.validate(new UpdateMatchStatusRequest("REJECTED", "khong khop model"))).isEmpty();
    }

    @Test
    void updateRecommendationStatusRequest_invalidStatus_violatesPattern() {
        assertThat(validator.validate(new UpdateRecommendationStatusRequest("PENDING", null))).isNotEmpty();
    }

    @Test
    void updateRecommendationStatusRequest_validStatuses_noViolation() {
        assertThat(validator.validate(new UpdateRecommendationStatusRequest("APPROVED", null))).isEmpty();
        assertThat(validator.validate(new UpdateRecommendationStatusRequest("REJECTED", "gia qua cao"))).isEmpty();
    }

    @Test
    void testCrawlRequest_blankUrl_violatesNotBlank() {
        assertThat(validator.validate(new TestCrawlRequest(""))).hasSize(1);
    }

    @Test
    void productUpdateRequest_negativePrice_violatesPositive() {
        assertThat(validator.validate(new ProductUpdateRequest(null, null, null, null, BigDecimal.valueOf(-1), true, null, null)))
                .isNotEmpty();
    }

    @Test
    void productUpdateRequest_nullPriceAndBlankOptionalFields_noViolation() {
        // Cac truong deu optional (chi validate KHI co gia tri) — giu dung nghiep vu partial-update hien tai.
        assertThat(validator.validate(new ProductUpdateRequest(null, null, null, null, null, false, null, null))).isEmpty();
    }

    @Test
    void productUpdateRequest_invalidAvailability_violatesPattern() {
        assertThat(validator.validate(new ProductUpdateRequest(null, null, null, null, null, false, "NOT_A_REAL_STATUS", null)))
                .isNotEmpty();
    }

    @Test
    void productUpdateRequest_validAvailability_noViolation() {
        assertThat(validator.validate(new ProductUpdateRequest(null, null, null, null, null, false, "IN_STOCK", null))).isEmpty();
    }
}
