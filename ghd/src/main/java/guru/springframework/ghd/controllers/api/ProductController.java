package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.product.*;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.services.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.utils.CommonUtil.safeSubstringByWord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product")
public class ProductController extends BaseController {

    private final ProductService productService;

    private static final String[][] HEADER_CONFIG = {
            {"id", "https://support.google.com/merchants/answer/6324405"},
            {"title", "https://support.google.com/merchants/answer/6324415"},
            {"description", "https://support.google.com/merchants/answer/6324468"},
            {"availability", "https://support.google.com/merchants/answer/6324448"},
            {"availability date", "https://support.google.com/merchants/answer/6324470"},
            {"expiration date", "https://support.google.com/merchants/answer/6324499"},
            {"shipping", null}, // #N/A
            {"google_product_category", null}, // #N/A
            {"link", "https://support.google.com/merchants/answer/6324416"},
            {"mobile link", "https://support.google.com/merchants/answer/6324459"},
            {"image link", "https://support.google.com/merchants/answer/6324350"},
            {"price", "https://support.google.com/merchants/answer/6324371"},
            {"sale price", "https://support.google.com/merchants/answer/6324471"},
            {"sale price effective date", "https://support.google.com/merchants/answer/6324460?hl=vi&ref_topic=6324338"},
            {"identifier exists", "https://support.google.com/merchants/answer/6324478"},
            {"gtin", "https://support.google.com/merchants/answer/6324461"},
            {"mpn", "https://support.google.com/merchants/answer/6324482"},
            {"brand", null}, // #N/A
            {"product highlight", "https://support.google.com/merchants/answer/9216100"},
            {"product detail", "https://support.google.com/merchants/answer/9218260"},
            {"additional image link", "https://support.google.com/merchants/answer/6324370"},
            {"condition", "https://support.google.com/merchants/answer/6324469"},
            {"adult", "https://support.google.com/merchants/answer/6324508"},
            {"color", "https://support.google.com/merchants/answer/6324487"},
            {"size", "https://support.google.com/merchants/answer/6324492"},
            {"size type", "https://support.google.com/merchants/answer/63244972"},
            {"size system", "https://support.google.com/merchants/answer/6324502"},
            {"gender", "https://support.google.com/merchants/answer/6324479"},
            {"material", "https://support.google.com/merchants/answer/6324410"},
            {"pattern", "https://support.google.com/merchants/answer/6324483"},
            {"age group", "https://support.google.com/merchants/answer/6324463"},
            {"multipack", "https://support.google.com/merchants/answer/6324488"},
            {"is bundle", "https://support.google.com/merchants/answer/6324449"},
            {"unit pricing measure", "https://support.google.com/merchants/answer/6324455"},
            {"unit pricing base measure", "https://support.google.com/merchants/answer/6324490"},
            {"energy efficiency class", "https://support.google.com/merchants/answer/7562785"},
            {"min energy efficiency class", "https://support.google.com/merchants/answer/7562785"},
            {"min energy efficiency class.1", "https://support.google.com/merchants/answer/7562785"},
            {"item group id", "https://support.google.com/merchants/answer/6324507"},
            {"sell on google quantity", "https://support.google.com/merchants/answer/9451716"}
    };

    @GetMapping("/search-suggestions")
    public ResponseEntity<?> getSuggestions(@RequestParam("ten") String ten) {
        List<ProductDetailClientResponse> suggestions = productService.findTop8ByTitleContaining(ten);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProduct(
            @RequestParam(name = "ten", required = false) String title,
            @RequestParam(name = "danh-muc", required = false) String categoryTitle,
            @RequestParam(name = "thuong-hieu", required = false) String brandTitle,
            @RequestParam(name = "gia-min", required = false) Double minPrice,
            @RequestParam(name = "gia-max", required = false) Double maxPrice,
            @RequestParam(name = "trang-thai", required = false, defaultValue = "1") Integer status,
            @RequestParam(name = "trang", defaultValue = DefaultPage.PAGE_STRING) int pageNumber,
            @RequestParam(name = "so-ban-ghi", defaultValue = DefaultPage.SIZE_STRING) int sizeNumber,
            @RequestParam(name = "thuoc-tinh", defaultValue = DefaultPage.CREATED_DATE) String sortField,
            @RequestParam(name = "kieu-sap-xep", defaultValue = DefaultPage.DESC) String sortDir
    ) {
        Page<ProductDetailClientResponse> productPage = productService.searchProduct(
                title, categoryTitle, brandTitle, minPrice, maxPrice, status, sortField, sortDir, pageNumber, sizeNumber
        );
        return ResponseEntity.ok(productPage);
    }

    @GetMapping("/find")
    public ResponseEntity<?> find(@RequestParam(name = "keyword", required = false) String keyword,
                                  @RequestParam(required = false) String excludeId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<ProductSearchDTO> products = productService.findByNameContainingIgnoreCase(keyword.trim());

        List<Map<String, Object>> result = products.stream()
                .filter(p -> excludeId == null || !p.getId().equals(excludeId))
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("body", result);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getProduct(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "updated_date") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        Page<ProductResponse> productPage = productService.getList(
                title, categoryId, brandId, status, sortField, sortDir, pageNumber, pageSize
        );

        return ResponseEntity.ok(productPage);
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> addProduct(
            @RequestPart("data") @Valid ProductRequest productRequest,
            @RequestPart(value = "mainImage", required = false) MultipartFile imageImage,
            @RequestPart(value = "gallery", required = false) List<MultipartFile> galleryFiles) {

        ProductResponse response = productService.addProduct(productRequest, imageImage, galleryFiles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getById(@PathVariable("productId") String productId) {

        ProductDetailResponse response = productService.getById(productId);

        return ok(response);
    }

    @PutMapping(value = "/{productId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateProduct(
            @PathVariable("productId") String productId,
            @RequestPart("data") @Valid ProductRequest productRequest,
            @RequestPart(value = "mainImage", required = false) MultipartFile imageImage,
            @RequestPart(value = "gallery", required = false) List<MultipartFile> galleryFiles
    ) {

        ProductResponse response = productService.updateProduct(productId, productRequest, imageImage, galleryFiles);

        return ok(response);
    }

    @DeleteMapping(value = "/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable("productId") String productId) {
        return ok(null);
    }

    @GetMapping("/sync")
    public ResponseEntity<?> syncCart(@RequestParam("listProductId") List<String> listProductId) {
        // Get information of product variant from database
        List<ProductDetailCartResponse> variants = productService.findByProductId(listProductId);

        return ok(variants);
    }

    @GetMapping("/export")
    public void exportProducts(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            HttpServletResponse response) throws IOException {

        // 1. Lấy dữ liệu
        List<ProductDetailClientResponse> products = productService.findAllForExport(title, categoryId, brandId);

        // 2. Thiết lập header response
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=google_merchant_products.xlsx");

        // 3. Tạo Workbook và Style
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Trang tính1");
        CreationHelper createHelper = workbook.getCreationHelper();

        // Style cho Hyperlink (Chữ xanh, gạch chân)
        CellStyle linkStyle = workbook.createCellStyle();
        Font linkFont = workbook.createFont();
        linkFont.setColor(IndexedColors.BLUE.getIndex());
        linkStyle.setFont(linkFont);

        // --- TẠO HEADER (DÒNG 1) VỚI CÔNG THỨC HYPERLINK ---
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADER_CONFIG.length; i++) {
            Cell cell = headerRow.createCell(i);
            String label = HEADER_CONFIG[i][0];
            String url = HEADER_CONFIG[i][1];

            if (url != null) {
                // Sử dụng setCellFormula để tạo hyperlink công thức y hệt file mẫu
                cell.setCellFormula("HYPERLINK(\"" + url + "\", \"" + label + "\")");
                cell.setCellStyle(linkStyle);
            } else {
                cell.setCellValue(label);
            }
        }

        // --- 4. ĐỔ DỮ LIỆU VÀO CÁC DÒNG ---
        int rowIdx = 1;
        for (ProductDetailClientResponse p : products) {
            Row row = sheet.createRow(rowIdx++);

            // id (0) & title (1)
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getTitle());

            // description (2) - Lọc bỏ HTML
            // 1. Loại bỏ HTML tags
            String plainDescription = p.getContent() != null ? p.getDescription().replaceAll("<[^>]*>", "") : "";

            // 2. Loại bỏ khoảng trắng thừa (nếu có) do quá trình strip HTML
            plainDescription = plainDescription.trim().replaceAll("\\s+", " ");

            // 3. Giới hạn số lượng ký tự (ví dụ: 1000 ký tự để tối ưu hiển thị)
            //plainDescription = safeSubstringByWord(plainDescription, 250).trim();

            row.createCell(2).setCellValue(plainDescription);

            // availability (3)
            row.createCell(3).setCellValue(p.getStockQty() != null && p.getStockQty() > 0 ? "in_stock" : "out_of_stock");

            // shipping (6) - Mặc định theo file mẫu
            row.createCell(6).setCellValue("VN:::0 VND");

            // google_product_category (7)
            row.createCell(7).setCellValue(p.getCategoryId());

            // --- link (8) - CÓ HYPERLINK CLICK ĐƯỢC ---
            String productUrl = "https://greenhomeshop.vn/san-pham/" + p.getSlug();
            createHyperlinkCell(row, 8, productUrl, productUrl, createHelper, linkStyle);

            // --- mobile link (9) ---
            //String mobileImageUrl = "https://greenhomeshop.vn" + p.getImage() + "_mobile.webp";
            // createHyperlinkCell(row, 9, productUrl, productUrl, createHelper, linkStyle);

            // --- image link (10) ---
            String fullImageUrl = "https://greenhomeshop.vn" + p.getImage() + "_pc.webp";
            createHyperlinkCell(row, 10, fullImageUrl, fullImageUrl, createHelper, linkStyle);

            // price (11) & sale price (12)
            row.createCell(11).setCellValue(p.getPrice().toBigInteger().toString() + " VND");

            if (p.getSalePrice() != null) {
                row.createCell(12).setCellValue(p.getSalePrice().toBigInteger().toString() + " VND");
            }

            // Updated to set the value to "no"
            row.createCell(14).setCellValue("no");

            // brand (17)
            row.createCell(17).setCellValue(p.getBrandName());

            // product detail (19)
            String spec = p.getSpecification();

            List<String> productDetails = new ArrayList<>();

            if (spec != null) {
                Document doc = Jsoup.parse(spec);
                Elements rows = doc.select("tr");

                String currentSection = "General";

                for (Element rowChild : rows) {
                    Elements cols = rowChild.select("td");

                    if (cols.size() == 3) {
                        String section = cols.get(0).text().trim();
                        String key = cols.get(1).text().trim();
                        String value = cols.get(2).text().trim();

                        if (!section.isEmpty()) {
                            currentSection = clean(section);
                        }

                        if (!key.isEmpty() && !value.isEmpty()) {
                            productDetails.add(formatDetail(currentSection, key, value));
                        }

                    } else if (cols.size() == 2) {
                        String key = cols.get(0).text().trim();
                        String value = cols.get(1).text().trim();

                        if (!key.isEmpty() && !value.isEmpty()) {
                            productDetails.add(formatDetail(currentSection, key, value));
                        }
                    }
                }
            }

            String result = productDetails.stream()
                    .limit(100)
                    .collect(Collectors.joining(","))
                    .replace("\n", "")
                    .replace("\r", "");

            for (String item : productDetails) {
                long count = item.chars().filter(ch -> ch == ':').count();
                if (count != 2) {
                    System.out.println("❌ Lỗi format: " + item);
                }
            }
            row.createCell(19).setCellValue(result);

            // --- additional image link (20) ---
            if (p.getListImages() != null && !p.getListImages().isEmpty()) {
                // 1. Chuyển đổi danh sách Object thành danh sách String URL hoàn chỉnh
                List<String> fullUrlList = p.getListImages().stream()
                        .filter(img -> img.getImageUrl() != null && !img.getImageUrl().isEmpty()) // Bảo vệ chống null field
                        .map(img -> {
                            String path = img.getImageUrl();
                            // Đảm bảo không bị thừa dấu / nếu path đã có sẵn ở đầu
                            String prefix = path.startsWith("/") ? "https://greenhomeshop.vn" : "https://greenhomeshop.vn/";
                            return prefix + path + "_pc.webp";
                        })
                        .toList();

                if (!fullUrlList.isEmpty()) {
                    // 2. Nối các URL lại bằng dấu phẩy (Dùng cho hiển thị và Google Merchant Center)
                    String adsImagesStr = fullUrlList.stream()
                            .limit(10)
                            .collect(Collectors.joining(","));
                    // 3. Lấy URL đầu tiên làm link click cho toàn bộ ô (Hyperlink ẩn)
                    String firstFullImg = fullUrlList.get(0);

                    // 4. Ghi vào file Excel
                    createHyperlinkCell(row, 20, adsImagesStr, firstFullImg, createHelper, linkStyle);
                }
            }

            // condition (21) & adult (22)
            row.createCell(21).setCellValue("new");
            row.createCell(22).setCellValue("no");

            // color (23) & size (24)
            row.createCell(23).setCellValue(p.getColor());
            row.createCell(24).setCellValue(p.getSize());

            // item group id (38)
            row.createCell(38).setCellValue(p.getGroupId());
        }

        // 5. Ghi dữ liệu ra stream
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * Hàm hỗ trợ tạo ô chứa Hyperlink hoạt động (click được)
     */
    private void createHyperlinkCell(Row row, int col, String value, String url, CreationHelper helper, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        Hyperlink link = helper.createHyperlink(HyperlinkType.URL);
        link.setAddress(url);
        cell.setHyperlink(link);
        cell.setCellStyle(style);
    }

    private String clean(String input) {
        if (input == null) return "";

        return input
                .replace(":", "-")        // bỏ dấu :
                .replace(",", ";")        // 🔥 đổi , -> ; (rất quan trọng)
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatDetail(String section, String key, String value) {
        return clean(section) + ":" + clean(key) + ":" + clean(value);
    }
}