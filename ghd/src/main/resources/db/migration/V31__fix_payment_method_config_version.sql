-- V30 insert 4 row sys_param cho payment-toggle nhưng thiếu cột version (bảng sys_param
-- không có DEFAULT cho version) -> version = NULL. SysParam.version là @Version
-- (optimistic lock) - Hibernate ném NullPointerException khi tăng version lúc UPDATE
-- (PUT /api/v1/sys-param -> SysParamServiceImpl.updateSysParam), vì current version
-- không thể unbox từ NULL. Không sửa lại V30 đã áp dụng (xem quy tắc Flyway) - set về 0
-- ở đây, đúng baseline mà JPA tự gán khi persist 1 entity mới qua code.
UPDATE sys_param
SET version = 0
WHERE param_key IN (
    'payment.cod.enabled',
    'payment.bank_transfer.enabled',
    'payment.card.enabled',
    'payment.installment.enabled'
)
AND version IS NULL;
