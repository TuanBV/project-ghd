-- Cho phep them loai import moi CSV_PRODUCT (import danh sach san pham co ban tu file CSV:
-- cot ID, SKU, Ten — khac voi feed Google Merchant Center day du cot cua loai MC).
ALTER TABLE import_runs DROP CONSTRAINT import_runs_import_type_check;
ALTER TABLE import_runs ADD CONSTRAINT import_runs_import_type_check
    CHECK (import_type IN ('MC', 'COMPARISON', 'CSV_PRODUCT'));
