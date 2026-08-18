---
name: spring-boot-layering
description: Ranh giới layer (controller/service/repository/dto/mapper/entity) và quy ước DTO-theo-feature của GHD. Đọc trước khi thêm/sửa bất kỳ endpoint, service method, hay entity nào.
---

# Layer boundary & DTO convention — GHD

Package gốc: `guru.springframework.ghd`. Cấu trúc thật (xem thêm
[docs/PROJECT_INDEX.md](../../../docs/PROJECT_INDEX.md)):

```
controllers/api      JSON controller, /api/v1/**, extends BaseController
controllers/views    Thymeleaf controller, /admin/v1/**, /**
services             Interface (vd CategoryService)
services/impl        Implementation (vd CategoryServiceImpl)
repositories          Spring Data JPA repository
entities              JPA entity, extends BaseEntity
dto/<feature>         Request/Response DTO theo feature (vd dto/category/CategoryRequest.java)
mappers               MapStruct mapper (entity <-> dto)
```

## Bảng ranh giới (hợp đồng, không phải gợi ý)

| Từ | Đến | Được phép? |
|---|---|---|
| `controllers/**` | `services/*Service` (interface, không phải impl) | ✅ |
| `controllers/**` | `repositories/*Repository` | ❌ **cấm, enforce bằng hook `.claude/hooks/layer-boundary.cjs`** — nếu bạn thêm `import guru.springframework.ghd.repositories.X` vào 1 file trong `controllers/**`, hook sẽ chặn ngay sau khi ghi file và bắt sửa lại |
| `controllers/**` | `entities/*` | ⚠️ đã có tiền lệ dùng lại ở vài nơi (`AuthenticationController`, `OrderController`, `ProductController`, `GlobalCommon`) — nhưng cho endpoint/API **mới**, mặc định trả DTO, không trả entity thẳng |
| `services/impl/**` | `repositories/*Repository` | ✅ (đây là nơi duy nhất nên gọi repository) |
| `services/impl/**` | `mappers/*Mapper` | ✅ map entity → DTO trước khi trả ra khỏi service |
| `dto/**` | `entities/**` | ❌ DTO không import ngược entity |

Vì sao ranh giới controller→repository là cấm cứng: đã grep toàn bộ
`controllers/**` khi xây dựng skill này — **0 vi phạm**, tức đây là invariant có thật
của codebase, không phải lý tưởng hoá. Giữ nguyên khi thêm code mới.

## Mẫu controller chuẩn (theo `CategoryController`)

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/<feature>")
public class XController extends BaseController {
    private final XService xService;

    @GetMapping
    public ResponseEntity<?> getList(...) { return ok(xService.getList(...)); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> add(@Valid @ModelAttribute XRequest req) { return ok(xService.add(req)); }
}
```

`BaseController` (`controllers/api/BaseController.java`) cung cấp `ok()`, `created()`,
`ng(message)`, `noContent()` — trả về `ApiResponse<T>` (`dto/ApiResponse.java`) thống
nhất. Dùng lại thay vì tự viết `ResponseEntity` tay. Lỗi tập trung ở
`controllers/api/ApiExceptionHandler.java` (API) và
`controllers/views/GlobalExceptionHandler.java` (view) — không tự try/catch generic
Exception trong controller trừ khi có lý do cụ thể.

Request có upload ảnh dùng `consumes = MULTIPART_FORM_DATA_VALUE` + `@ModelAttribute`
(không phải `@RequestBody`) — theo đúng pattern `CategoryController`/`BrandController`.

## MapStruct + Lombok — điểm dễ quên

- `pom.xml` cấu hình `-Amapstruct.defaultComponentModel=Spring` trong
  `maven-compiler-plugin` → mapper interface **không cần** tự khai báo
  `@Mapper(componentModel = "spring")`, chỉ cần `@Mapper` trơn là đủ (nhưng khai báo
  tường minh cũng không sai, chỉ dư).
- Thứ tự annotation processor path trong `pom.xml`: `mapstruct-processor` →
  `lombok` → `lombok-mapstruct-binding`. Nếu thêm dependency mới sinh code (annotation
  processor khác), giữ đúng thứ tự này, đừng chèn giữa — sai thứ tự khiến Lombok-sinh
  getter/setter không kịp có mặt khi MapStruct generate implementation, build sẽ lỗi
  khó hiểu (missing method) chứ không lỗi rõ ràng ngay dòng cấu hình.
- Sau khi thêm/sửa field trong entity hoặc DTO có mapper tương ứng, phải
  `.\mvnw.cmd compile` lại để MapStruct regenerate — implementation cũ trong
  `target/generated-sources` không tự cập nhật nếu IDE cache annotation processing.

## Checklist

Xem [checklist.md](checklist.md) khi thêm 1 feature mới hoặc sửa layer hiện có.
