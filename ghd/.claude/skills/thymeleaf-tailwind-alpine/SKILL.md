---
name: thymeleaf-tailwind-alpine
description: Cấu trúc template Thymeleaf (admin/client/common), build Tailwind CSS v4 + Alpine.js của GHD. Đọc trước khi sửa template hoặc CSS/JS phía client.
---

# Thymeleaf + Tailwind v4 + Alpine.js — GHD

## Cấu trúc template thật

```
templates/admin/         Trang quản trị (layout.html là layout chính, auth/sign-in.html riêng)
templates/admin/product/, templates/admin/news/   Sub-feature nhiều trang (detail/list/preview)
templates/client/        Trang khách hàng (home, about, contact, cart, product, news, policy)
templates/common/admin/  Fragment dùng chung phần admin (header, navbar, sidebar)
templates/common/client/ Fragment dùng chung phần client (header, footer, breadcrumb, floating-contact, gtm)
templates/common/head.html   Fragment <head> dùng chung cả 2 phía
templates/error/          404.html, 500.html
```

Static assets tương ứng: `static/admin`, `static/client`, `static/common` (font, icon
Zalo/Messenger dùng chung, sitemap.xml, robots.txt ở `static/`).

Controller tương ứng: `controllers/views/ViewAdminController.java` (`/admin/v1/**`),
`controllers/views/ViewClientController.java` (`/`, route tiếng Việt như
`/san-pham`, `/tin-tuc`, `/gio-hang`...). `GlobalCommon`
(`@ControllerAdvice`) chỉ áp dụng cho 2 controller này — bơm `navBarData`, `hotline`,
`sysParams`, `currentUrl` vào model qua `@ModelAttribute`. Route index đầy đủ:
[docs/PROJECT_INDEX.md](../../../docs/PROJECT_INDEX.md) mục "View Route Index".

**Quan trọng**: `GlobalCommon` từng là advice không giới hạn phạm vi, chạy nhầm cả trên
API JSON (`controllers/api`), gây load thừa category/brand/product mỗi request API và
nhiễu 1 pessimistic lock (đơn hàng). Đã fix bằng cách giới hạn advice chỉ áp dụng
`controllers.views`. Không mở rộng lại phạm vi `@ControllerAdvice` này ra ngoài
`controllers.views` mà không hiểu rõ hệ quả.

## Build frontend (Tailwind v4 CLI + Alpine.js)

`package.json` (root `ghd/`, không phải root git):

```json
"scripts": {
  "build:css": "tailwindcss -i ./src/main/frontend/app.css -o ./src/main/resources/static/client/css/app.css --minify",
  "copy:alpine": "node -e \"...copy node_modules/alpinejs/dist/cdn.min.js -> static/client/js/alpine.min.js\"",
  "build:frontend": "npm run build:css && npm run copy:alpine"
}
```

- Nguồn Tailwind input: `src/main/frontend/app.css`. Output đã minify:
  `src/main/resources/static/client/css/app.css` — **không sửa tay file output này**,
  sửa input rồi build lại.
- Alpine.js không cài qua CDN runtime — được copy file `.min.js` từ
  `node_modules/alpinejs` vào `static/client/js/alpine.min.js` lúc build. Nâng cấp
  version Alpine = đổi `package.json`, chạy lại `npm run copy:alpine`, không tự sửa
  file `.min.js` đã copy.
- **Không cần Node.js lúc runtime** — frontend build diễn ra trong Docker image lúc
  `docker compose build`, không phải lúc container chạy. Node chỉ cần khi dev local
  ngoài Docker.
- Sau khi sửa `src/main/frontend/app.css` hoặc thêm class Tailwind mới trong template,
  chạy `npm run build:css` (hoặc `npm run build:frontend`) trước khi coi thay đổi UI
  là xong — nếu không, class mới không có trong CSS output và UI sẽ không đổi khi test
  trên trình duyệt.

## Khi sửa template

- Giữ nguyên convention fragment `th:replace`/`th:insert` đang dùng trong
  `templates/common/**` — không tự tạo hệ thống fragment song song.
  Đừng thay đổi phạm vi `@ControllerAdvice` của `GlobalCommon` nêu trên.
