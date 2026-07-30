package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.DelFlag;
import guru.springframework.ghd.dto.slider.SliderRequest;
import guru.springframework.ghd.dto.slider.SliderResponse;
import guru.springframework.ghd.entities.Slider;
import guru.springframework.ghd.mappers.SliderMapper;
import guru.springframework.ghd.repositories.SliderRepository;
import guru.springframework.ghd.services.SliderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.config.CacheConfig.SLIDERS;
import static guru.springframework.ghd.utils.PaginationUtil.getPageRequest;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class SliderServiceImpl implements SliderService {

    private final SliderRepository sliderRepository;
    private final SliderMapper sliderMapper;

    private final String SUB_FOLDER = "sliders";

    @Override
    public Page<SliderResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = getPageRequest(pageNumber, pageSize, sortField, sortDir);
        Page<Slider> sliderPage;

        if (StringUtils.hasText(title)) {
            sliderPage = sliderRepository.findAllByTitleContainingIgnoreCase(title, pageRequest);
        } else {
            sliderPage = sliderRepository.findByDelFlag(DelFlag.ACTIVE.get(), pageRequest);
        }

        return sliderPage.map(sliderMapper::sliderToSliderResponse);
    }

    @Override
    public Optional<SliderResponse> getById(String sliderId) {
        return sliderRepository.findById(UUID.fromString(sliderId))
                .map(sliderMapper::sliderToSliderResponse);
    }

    @Override
    @Cacheable(cacheNames = SLIDERS, key = "'all'")
    public List<SliderResponse> getAll() {
        List<Slider> sliders = sliderRepository.findAllByDelFlag(DelFlag.ACTIVE.get());

        return sliders.stream().map(sliderMapper::sliderToSliderResponse).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = SLIDERS, allEntries = true)
    public SliderResponse addSlider(SliderRequest request) {
        Slider slider = Slider.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .linkUrl(request.getLinkUrl())
                .position(request.getPosition())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String filePath = handleImageUpload(request.getImageFile(), SUB_FOLDER);
            slider.setImageUrl(filePath);
        }

        return sliderMapper.sliderToSliderResponse(sliderRepository.save(slider));
    }

    @Override
    @CacheEvict(cacheNames = SLIDERS, allEntries = true)
    public Optional<SliderResponse> updateById(String sliderId, SliderRequest request) {
        return sliderRepository.findById(UUID.fromString(sliderId)).map(existingSlider -> {
            existingSlider.setTitle(request.getTitle());
            existingSlider.setDescription(request.getDescription());
            existingSlider.setLinkUrl(request.getLinkUrl());
            existingSlider.setPosition(request.getPosition());
            existingSlider.setIsActive(request.getIsActive());

            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                existingSlider.setImageUrl(handleImageUpload(request.getImageFile(), SUB_FOLDER));
            }

            return sliderMapper.sliderToSliderResponse(sliderRepository.save(existingSlider));
        });
    }

    @Override
    @CacheEvict(cacheNames = SLIDERS, allEntries = true)
    public void deleteById(String sliderId) {
         sliderRepository.findById(UUID.fromString(sliderId)).map(existingSlider -> {
            existingSlider.setDelFlag(DelFlag.NOT_ACTIVE.get());

            return sliderRepository.save(existingSlider);
        });
    }
}