package com.example.mcprice.repository;

import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.Product;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("skuOriginal")), pattern),
                cb.like(cb.lower(root.get("productUrl")), pattern));
    }

    public static Specification<Product> category(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("googleCategory"), category);
    }

    public static Specification<Product> availability(String availability) {
        if (!StringUtils.hasText(availability)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("availability"), availability);
    }

    public static Specification<Product> active(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    /**
     * Uu tien sap xep danh sach san pham theo muc do can nguoi dung xu ly:
     * 1) San pham co it nhat 1 URL doi thu dang REVIEW_REQUIRED (can xac nhan co khop hay khong)
     *    len dau — day la thong tin nguoi dung can biet ngay de xu ly.
     * 2) Trong so con lai, san pham "Con hang" (IN_STOCK) len truoc.
     * 3) Cuoi cung sap theo id de on dinh thu tu giua cac lan phan trang.
     *
     * Gop ca 3 tieu chi vao 1 lan goi query.orderBy(...) vi moi lan goi se THAY THE (khong cong don)
     * thu tu sap xep da dat truoc do, nen khong the tach thanh nhieu Specification rieng biet.
     *
     * Bo qua khi dang build count-query (ket qua la Long) — ORDER BY theo cot khong nam trong
     * ham tong hop se loi cu phap SQL khi khong co GROUP BY.
     */
    public static Specification<Product> orderByReviewNeededThenInStock() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                Subquery<Long> reviewNeededListing = query.subquery(Long.class);
                Root<CompetitorListing> listing = reviewNeededListing.from(CompetitorListing.class);
                reviewNeededListing.select(listing.get("id"))
                        .where(cb.equal(listing.get("product"), root),
                                cb.equal(listing.get("matchStatus"), MatchStatus.REVIEW_REQUIRED));

                Expression<Integer> reviewNeededFirst = cb.<Integer>selectCase()
                        .when(cb.exists(reviewNeededListing), 0)
                        .otherwise(1);
                Expression<Integer> inStockFirst = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("availability"), "IN_STOCK"), 0)
                        .otherwise(1);
                query.orderBy(cb.asc(reviewNeededFirst), cb.asc(inStockFirst), cb.asc(root.get("id")));
            }
            return cb.conjunction();
        };
    }

    public static Specification<Product> idIn(Collection<Long> ids) {
        if (ids == null) {
            return null;
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }

    @SafeVarargs
    public static Specification<Product> allOf(Specification<Product>... specs) {
        Specification<Product> result = Specification.where(null);
        for (Specification<Product> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
