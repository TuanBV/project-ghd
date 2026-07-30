package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.slider.SliderRequest;
import guru.springframework.ghd.dto.slider.SliderResponse;
import guru.springframework.ghd.entities.Slider;
import org.springframework.data.domain.Page;

import java.util.*;

public interface SliderService {

    Page<SliderResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Optional<SliderResponse> getById(String sliderId);

    Optional<SliderResponse> updateById(String sliderId, SliderRequest sliderRequest);

    void deleteById(String sliderId);

    List<SliderResponse> getAll();

    SliderResponse addSlider(SliderRequest sliderRequest);
}