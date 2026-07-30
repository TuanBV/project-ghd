package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.banner.BannerRequest;
import guru.springframework.ghd.dto.banner.BannerResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BannerService {

    Page<BannerResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Optional<BannerResponse> getById(UUID bannerId);

    Optional<BannerResponse> updateById(UUID bannerId, BannerRequest bannerRequest);

    void deleteById(UUID bannerId);

    List<BannerResponse> getAll();

    BannerResponse addBanner(BannerRequest bannerRequest);

    BannerResponse getBannerByPosition(String position);
}