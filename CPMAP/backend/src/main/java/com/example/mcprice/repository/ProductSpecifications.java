package com.example.mcprice.repository;

import com.example.mcprice.domain.Product;
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
