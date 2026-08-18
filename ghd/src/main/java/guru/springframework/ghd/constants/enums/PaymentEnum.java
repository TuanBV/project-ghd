package guru.springframework.ghd.constants.enums;

public enum PaymentEnum {
    BANK_TRANSFER, COD,
    // Thanh toán online qua VNPay - khách chọn cụ thể ngân hàng/thẻ/đối tác trả góp
    // ngay trên trang VNPay, GHD chỉ cần phân biệt 2 nhánh này cho UI/Telegram/admin.
    CARD, INSTALLMENT
}