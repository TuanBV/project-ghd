package guru.springframework.ghd.services;


import guru.springframework.ghd.dto.brand.BrandRequest;
import guru.springframework.ghd.dto.brand.BrandResponse;
import org.springframework.data.domain.Page;

import java.util.*;

public interface BrandService {
    Page<BrandResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Optional<BrandResponse> getById(String brandId);

    Optional<BrandResponse> updateById(String brandId, BrandRequest brand);

    void deleteById(String brandId);

    BrandResponse addBrand(BrandRequest brand);

    List<BrandResponse> getAll();

    List<BrandResponse> getAllNotNullLogo();
}
