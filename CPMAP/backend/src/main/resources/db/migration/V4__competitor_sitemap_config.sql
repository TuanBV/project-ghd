-- Bo sung sitemapUrl vao extractor_config cho 3 doi thu duoc phep doc sitemap.xml theo robots.txt
-- (da kiem tra thuc te: ca 3 site deu Disallow trang tim kiem noi bo nhung KHONG Disallow sitemap
-- hay tung trang san pham rieng le, nen dung sitemap.xml de kham pha URL la hop le va tuan thu).
-- dienmayabc.com KHONG duoc bo sung: robots.txt cua ho chan thang "ClaudeBot" va hau het AI bot
-- tren toan site, nen tiep tuc giu MANUAL_ONLY, khong tu dong kham pha/crawl.

UPDATE competitors
SET extractor_config = extractor_config || '{"sitemapUrl": "https://sgt.com.vn/sitemap.xml"}'::jsonb
WHERE name = 'sgt.com.vn';

UPDATE competitors
SET extractor_config = extractor_config || '{"sitemapUrl": "https://dienmay88.vn/sitemap_index.xml"}'::jsonb
WHERE name = 'dienmay88.vn';

UPDATE competitors
SET extractor_config = extractor_config || '{"sitemapUrl": "https://dienmaythienphu.vn/sitemap.xml"}'::jsonb
WHERE name = 'dienmaythienphu.vn';
