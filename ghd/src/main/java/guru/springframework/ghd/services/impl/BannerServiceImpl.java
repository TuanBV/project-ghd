package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.DelFlag;
import guru.springframework.ghd.dto.banner.BannerRequest;
import guru.springframework.ghd.dto.banner.BannerResponse;
import guru.springframework.ghd.entities.Banner;
import guru.springframework.ghd.mappers.BannerMapper;
import guru.springframework.ghd.repositories.BannerRepository;
import guru.springframework.ghd.services.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static guru.springframework.ghd.config.CacheConfig.BANNERS;
import static guru.springframework.ghd.utils.PaginationUtil.getPageRequest;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;

    private final String SUB_FOLDER = "banners";

    @Override
    public Page<BannerResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = getPageRequest(pageNumber, pageSize, sortField, sortDir);
        Page<Banner> bannerPage;

        if (StringUtils.hasText(title)) {
            bannerPage = bannerRepository.findAllByTitleContainingIgnoreCaseAndDelFlag(title, DelFlag.ACTIVE.get(), pageRequest);
        } else {
            bannerPage = bannerRepository.findByDelFlag(DelFlag.ACTIVE.get(), pageRequest);
        }

        return bannerPage.map(bannerMapper::bannerToBannerResponse);
    }

    @Override
    public Optional<BannerResponse> getById(UUID bannerId) {
        return bannerRepository.findById(bannerId)
                .map(bannerMapper::bannerToBannerResponse);
    }

    @Override
    @Cacheable(cacheNames = BANNERS, key = "'all'")
    public List<BannerResponse> getAll() {
        List<Banner> banners = bannerRepository.findAllByDelFlag(DelFlag.ACTIVE.get());
        return banners.stream()
                .map(bannerMapper::bannerToBannerResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = BANNERS, allEntries = true)
    public BannerResponse addBanner(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .linkUrl(request.getLinkUrl())
                .position(request.getPosition())
                .isActive(request.getIsActive() != null ? request.getIsActive() : 0) // Default 0 (Inactive)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .delFlag(DelFlag.ACTIVE.get())
                .build();

        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String filePath = handleImageUpload(request.getImageFile(), SUB_FOLDER);
            banner.setImageUrl(filePath);
        }

        return bannerMapper.bannerToBannerResponse(bannerRepository.save(banner));
    }

    @Override
    @CacheEvict(cacheNames = BANNERS, allEntries = true)
    public Optional<BannerResponse> updateById(UUID bannerId, BannerRequest request) {
        return bannerRepository.findById(bannerId).map(existingBanner -> {
            existingBanner.setTitle(request.getTitle());
            existingBanner.setLinkUrl(request.getLinkUrl());
            existingBanner.setPosition(request.getPosition());
            existingBanner.setIsActive(request.getIsActive());
            existingBanner.setStartDate(request.getStartDate());
            existingBanner.setEndDate(request.getEndDate());

            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                existingBanner.setImageUrl(handleImageUpload(request.getImageFile(), SUB_FOLDER));
            }

            return bannerMapper.bannerToBannerResponse(bannerRepository.save(existingBanner));
        });
    }

    @Override
    @CacheEvict(cacheNames = BANNERS, allEntries = true)
    public void deleteById(UUID bannerId) {
        bannerRepository.findById(bannerId).ifPresent(existingBanner -> {
            existingBanner.setDelFlag(DelFlag.NOT_ACTIVE.get()); // Soft delete: set del_flag = 1
            bannerRepository.save(existingBanner);
        });
    }

    @Override
    @Cacheable(cacheNames = BANNERS, key = "'position:' + #position")
    public BannerResponse getBannerByPosition(String position) {
        return bannerRepository.findByPositionAndIsActiveAndDelFlag(position, 1, DelFlag.ACTIVE.get())
                .map(bannerMapper::bannerToBannerResponse)
                .orElse(null);
    }
}