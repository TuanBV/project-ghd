# GHD Project Index

Last indexed: 2026-06-10

## Overview

GHD is a Spring Boot Java 17 application for an ecommerce-style site with an admin dashboard and public client pages. It uses Spring MVC, Thymeleaf, Spring Security, JPA, Flyway, MySQL, Lombok, MapStruct, JWT, image processing, mail, Telegram notifications, and Redis (Spring Cache + Spring Session). It is containerized with Docker/Docker Compose - see `docs/DOCKER.md`.

Main entry point:

- `src/main/java/guru/springframework/ghd/GhdApplication.java`

Build file:

- `pom.xml`

## Runtime And Configuration

- Application config: `src/main/resources/application.properties` (values are environment-variable-driven with local defaults; see `.env.example`)
- Database: MySQL at schema `core`
- Migrations: `src/main/resources/db/migration`
- Upload root: configurable via `upload.path`/`UPLOAD_PATH` (default `uploads`)
- SEO static files: configurable via `seo.static.path`/`SEO_STATIC_PATH` (default `seo`)
- Logs: configurable via `LOG_PATH` (default `logs/app.log`)
- Static sitemap: `src/main/resources/static/sitemap.xml`
- Static robots file: `src/main/resources/static/robots.txt`
- Cache/session: Redis-backed, see `config/CacheConfig.java` and `config/SessionConfig.java`
- Docker: `Dockerfile`, `compose.yaml`, `compose.debug.yaml`, `docs/DOCKER.md`

`application.properties` no longer contains real secrets - values come from environment variables (`.env` locally/in Docker). Do not commit `.env`.

## Source Layout

- `src/main/java/guru/springframework/ghd/config`: MVC, security, converters, optional OpenAPI config.
- `src/main/java/guru/springframework/ghd/constants`: pagination defaults, request headers, enums.
- `src/main/java/guru/springframework/ghd/controllers/api`: JSON/API controllers under `/api/v1`.
- `src/main/java/guru/springframework/ghd/controllers/views`: Thymeleaf page controllers and global model/exception handling.
- `src/main/java/guru/springframework/ghd/dto`: request/response/projection DTOs grouped by feature.
- `src/main/java/guru/springframework/ghd/entities`: JPA entities.
- `src/main/java/guru/springframework/ghd/mappers`: MapStruct mappers.
- `src/main/java/guru/springframework/ghd/repositories`: Spring Data repositories and native/query-heavy search methods.
- `src/main/java/guru/springframework/ghd/security`: authentication filters and security support.
- `src/main/java/guru/springframework/ghd/services`: service interfaces plus shared services.
- `src/main/java/guru/springframework/ghd/services/impl`: feature service implementations.
- `src/main/java/guru/springframework/ghd/utils`: upload, cookie, AES, pagination, and common helpers.
- `src/main/java/guru/springframework/ghd/validations`: custom validation annotations and validators.
- `src/main/resources/templates`: Thymeleaf templates for admin, client, common fragments, and errors.
- `src/main/resources/static`: CSS, JS, images, fonts, icons, robots, sitemap.
- `scripts`: local database helper scripts.
- `uploads`: runtime uploaded media.

## Main Domain Entities

- Catalog: `Product`, `ProductImage`, `ProductSimilar`, `Category`, `Brand`
- Content: `News`, `Banner`, `Slider`, `Policy`
- Commerce: `Orders`, `OrderDetail`
- Customer/admin interaction: `Contact`, `Review`, `ReviewImage`
- Users/security: `User`, `UserImage`
- System config: `SysParam`
- Base model: `BaseEntity`

Most entities extend `BaseEntity`, which centralizes IDs and audit/delete metadata.

## Backend Layers By Feature

### Product

- API: `controllers/api/ProductController.java`
- Service: `services/ProductService.java`, `services/impl/ProductServiceImpl.java`
- Repository: `repositories/ProductRepository.java`, `ProductImageRepository.java`, `ProductSimilarRepository.java`
- DTOs: `dto/product`
- Mapper: `mappers/ProductMapper.java`, `ProductImageMapper.java`, `ProductVariantMapper.java`
- Admin templates: `templates/admin/product`
- Client templates: `templates/client/product`

### Category

- API: `controllers/api/CategoryController.java`
- Service: `CategoryService`, `CategoryServiceImpl`
- Repository: `CategoryRepository`
- DTOs: `dto/category`
- Mapper: `CategoryMapper`
- Admin template: `templates/admin/category.html`

### Brand

- API: `controllers/api/BrandController.java`
- Service: `BrandService`, `BrandServiceImpl`
- Repository: `BrandRepository`
- DTOs: `dto/brand`
- Mapper: `BrandMapper`
- Admin template: `templates/admin/brand.html`

### News

- API: `controllers/api/NewsController.java`
- Service: `NewsService`, `NewsServiceImpl`
- Repository: `NewsRepository`
- DTOs: `dto/news`
- Mapper: `NewsMapper`
- Admin templates: `templates/admin/news`
- Client templates: `templates/client/news`

### Orders

- API: `controllers/api/OrderController.java`
- Service: `OrdersService`, `OrdersServiceImpl`
- Repositories: `OrdersRepository`, `OrderDetailRepository`
- DTOs: `dto/order`
- Mappers: `OrderMapper`, `OrderDetailMapper`
- Admin template: `templates/admin/order.html`
- Client cart template: `templates/client/cart.html`

### Users And Auth

- Auth API: `controllers/api/AuthenticationController.java`
- User API: `controllers/api/UserController.java`
- Services: `AuthenticationServiceImpl`, `UserServiceImpl`, `CustomerUserDetailsService`, `JwtService`
- Repositories: `AuthenticationRepository`, `UserRepository`, `UserImagesRepository`
- DTOs: `dto/auth`, `dto/user`
- Security config: `config/SecurityConfig.java`
- Admin templates: `templates/admin/auth/sign-in.html`, `templates/admin/user.html`, `templates/admin/profile.html`

### Banners, Sliders, Policies, Reviews, Contacts, Sys Params

- API controllers: `BannerController`, `SliderController`, `PolicyController`, `ReviewController`, `ContactController`, `SysParamController`
- Services follow the same `FeatureService` / `FeatureServiceImpl` pattern.
- Repositories follow the same `FeatureRepository` pattern.
- Admin templates are mostly single files under `templates/admin`.
- Public policy/contact pages live under `templates/client`.

## API Route Index

Base API routes:

- `/api/v1/auth`: login/logout.
- `/api/v1/user`: user listing, creation, detail, current user, profile update, password change.
- `/api/v1/product`: product listing, creation, update, delete, search, suggestions, export, sync.
- `/api/v1/category`: category CRUD.
- `/api/v1/brand`: brand CRUD.
- `/api/v1/banner`: banner CRUD.
- `/api/v1/slider`: slider CRUD.
- `/api/v1/news`: news listing and CRUD.
- `/api/v1/order`: order creation, detail, update, list.
- `/api/v1/contact`: contact list, creation, detail, status update.
- `/api/v1/policy`: policy CRUD.
- `/api/v1/review`: review listing and creation.
- `/api/v1/sys-param`: system parameter read/update and sitemap generation.
- Image upload route: `controllers/api/ImageController.java`.

Common API response helpers:

- `controllers/api/BaseController.java`
- `controllers/api/ApiExceptionHandler.java`
- `dto/ApiResponse.java`
- `dto/ErrorResponse.java`

## View Route Index

Admin pages are handled by `controllers/views/ViewAdminController.java` under `/admin/v1/`:

- Dashboard/home: `/admin/v1/`
- Users: `/admin/v1/user`
- Categories: `/admin/v1/category`
- Brands: `/admin/v1/brand`
- Products: `/admin/v1/product`, `/admin/v1/product/add`, `/admin/v1/product/edit/{id}`, `/admin/v1/product/duplicate/{id}`, `/admin/v1/product/preview`
- Sliders: `/admin/v1/slider`
- Banners: `/admin/v1/banner`
- Policies: `/admin/v1/policy`
- News: `/admin/v1/news`, `/admin/v1/news/add`, `/admin/v1/news/edit/{id}`, `/admin/v1/news/preview`
- Orders: `/admin/v1/order`
- Contacts: `/admin/v1/contact`
- Settings: `/admin/v1/settings`
- Profile: `/admin/v1/profile`
- Sign in: `/admin/v1/sign-in`

Client pages are handled by `controllers/views/ViewClientController.java` under `/`:

- Home: `/`
- About: `/ve-chung-toi`
- Contact: `/lien-he`
- News list/detail: `/tin-tuc`, `/tin-tuc/{slug}`
- Products list/detail: `/san-pham`, `/san-pham/{slug}`, legacy `/product/{idProduct}`
- Cart: `/gio-hang`
- Policies: `/chinh-sach/doi-tra`, `/chinh-sach/bao-hanh`, `/chinh-sach/thanh-toan`, `/chinh-sach/dieu-khoan-su-dung-va-bao-mat`, `/chinh-sach/quy-dinh-chung`, `/chinh-sach/van-chuyen`

Shared view support:

- `controllers/views/GlobalCommon.java`
- `controllers/views/GlobalExceptionHandler.java`
- Common templates: `templates/common`
- Error templates: `templates/error/404.html`, `templates/error/500.html`

## Templates And Assets

Admin templates:

- `templates/admin/layout.html`
- `templates/admin/home.html`
- `templates/admin/auth/sign-in.html`
- Feature pages: `banner.html`, `brand.html`, `category.html`, `contact.html`, `order.html`, `policy.html`, `profile.html`, `settings.html`, `slider.html`, `user.html`
- Product pages: `templates/admin/product/detail.html`, `list.html`, `preview.html`
- News pages: `templates/admin/news/detail.html`, `list.html`, `preview.html`

Client templates:

- `templates/client/home.html`
- `templates/client/about.html`
- `templates/client/contact.html`
- `templates/client/cart.html`
- `templates/client/slider.html`
- Product pages: `templates/client/product/detail.html`, `item_fragment.html`, `list.html`
- News pages: `templates/client/news/detail.html`, `list.html`
- Policy pages: `templates/client/policy`

Common fragments:

- Admin: `templates/common/admin/header.html`, `navbar.html`, `sidebar.html`
- Client: `templates/common/client/header.html`, `footer.html`, `breadcrumb.html`, `floating-contact.html`, `gtm.html`
- Shared head: `templates/common/head.html`

Static assets:

- Admin: `static/admin`
- Client: `static/client`
- Shared: `static/common`

## Database And Migrations

Flyway migrations live in `src/main/resources/db/migration` and currently run from `V1__init-mysql.sql` through `V25__alter_product.sql`.

High-level migration areas:

- `V1`: initial MySQL schema.
- `V2`-`V8`: user images, brand, category, contact, sys params, news, slider.
- `V9`-`V10`: product and product image.
- `V11`-`V14`: orders, banner, order/user alterations.
- `V15`-`V18`: policy/product/news alterations.
- `V19`: review and review image.
- `V20`-`V25`: product, product similar, category/brand, product image alterations.

There is also a database dump at `dump-core-202606100141.sql` and a helper script at `scripts/mysql-init.sql`.

## Build, Run, Test

Build/test with Maven wrapper:

```powershell
.\mvnw.cmd test
```

Run locally:

```powershell
.\mvnw.cmd spring-boot:run
```

The app expects MySQL to be available at the configured local connection and Flyway validation/migration to succeed. The default web port is Spring Boot's standard `8080` unless overridden outside this repo.

## Current Test Coverage

Only one basic test class exists:

- `src/test/java/guru/springframework/ghd/ManagerUserApplicationTests.java`

Most behavior is currently covered by application structure rather than focused unit/integration tests. Be careful around product search, upload handling, authentication/security, and order flows because they touch multiple layers.

## Hotspots

- `ProductController`, `ProductServiceImpl`, and `ProductRepository` are the largest and most behavior-heavy product files.
- `ViewClientController` contains many public page flows and product detail/list logic.
- `application.properties` is environment-variable-driven (see `.env.example`); no real secrets are committed.
- Uploaded files are stored under `uploads`, while static bundled assets are under `src/main/resources/static`.
- Some templates are large and include page-level JavaScript, especially admin product/news pages and client product detail/list pages.
- `src/main/resources/templates/client/product/detail.html` has local uncommitted changes as of this index.

